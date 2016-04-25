package io.hops.kafka;

import io.hops.metadata.hdfs.entity.User;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import se.kth.bbc.project.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.util.Settings;
import kafka.admin.AdminUtils;
import kafka.common.TopicExistsException;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkConnection;

@Stateless
public class KafkaFacade {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @EJB
    Settings settings;
    
    public static final String SEPARATOR = ":";

    private  Set<String> zkBrokerList;
    private  Set<String> topicList;

    protected EntityManager getEntityManager() {
        return em;
    }

    public KafkaFacade() throws AppException, Exception {
    }

    /**
     * Get all the Topics for the given project.
     * <p/>
     * @param project
     * @return
     */
    public List<TopicDTO> findTopicsByProject(Project project) {
        TypedQuery<ProjectTopics> query = em.createNamedQuery(
                "ProjectTopics.findByProject",
                ProjectTopics.class);
        query.setParameter("project_id", project.getId());
        List<ProjectTopics> res = query.getResultList();
        List<TopicDTO> topics = new ArrayList<>();
        for (ProjectTopics pt : res) {
            topics.add(new TopicDTO(pt.getTopicName()));
        }
        return topics;
    }

    public TopicDetailDTO getTopicDetails(Project project, String topicName)
            throws AppException, Exception {
        List<TopicDTO> topics = findTopicsByProject(project);
        if (topics.isEmpty()) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "No Kafka topics found in this project.");
        }
        for (TopicDTO topic : topics) {
            if (topic.getTopic().compareToIgnoreCase(topicName) == 0) {
                TopicDetailDTO topicDetailDTO = getTopicDetailsfromKafkaCluster(topicName);
                return topicDetailDTO;
            }
        }

        return new TopicDetailDTO();
    }

    private int getPort(String zkIp) {
        String[] split= zkIp.split(SEPARATOR, 2);
       int zkPort= Integer.parseInt(split[1]);
        return zkPort;
    }
    
    private InetAddress getIp(String zkIp) throws AppException {
        String[] split= zkIp.split(SEPARATOR, 2);
        String ip = split[0];
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException ex) {
            throw new AppException(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
                    "Zookeeper service is not available right now...");
        }
    }

    //this should return list of projects the topic belongs to as owner or shared
    public List<Project> findProjectforTopic(String topicName)
            throws AppException {
        TypedQuery<ProjectTopics> query = em.createNamedQuery(
                "ProjectTopics.findByTopicName", ProjectTopics.class);
        query.setParameter("topic_name", topicName);
        
        if (query == null) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "No project found for this Kafka topic.");
        }
        List<ProjectTopics> resp = query.getResultList();
        List<Project> projects = new ArrayList<>();
        for (ProjectTopics id : resp) {
            Project p = em.find(Project.class, id.getId());
            if (p != null) {
                projects.add(p);
            }
        }
        
        if (projects.isEmpty()) {
            throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                    "No project found for this Kafka topic.");
        }
        
        return projects;
    }

    public void createTopicInProject(Project project, String topicName) 
            throws AppException {
        ProjectTopics pt = em.find(ProjectTopics.class, 
                new ProjectTopicsPK(topicName, project.getId()));
        
        if (pt != null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Kafka topic already exists. Pick a different topic name.");
        }

        // create the topic in kafka if project is owner
        if(pt.getOwner()){
        ZkClient zkClient = new ZkClient(getIp(settings.getZkIp()).getHostName(), 
                10*1000, 29*1000, ZKStringSerializer$.MODULE$);
        ZkConnection zkConnection = new ZkConnection(settings.getZkIp());       
        ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);
        
        try{
            if(!AdminUtils.topicExists(zkUtils, topicName)){
                AdminUtils.createTopic(zkUtils, "testzk", 3, 1, new Properties());
            }
        }catch(TopicExistsException ex){
            throw new AppException(Response.Status.FOUND.getStatusCode(), 
                    "Kafka topic already exists. Pick a different topic name.");
        }finally{
            zkClient.close();
        }
        }
        //persist topic into database
        pt = new ProjectTopics(topicName, project.getId(), true);
        em.merge(pt);
        em.persist(pt);
        em.flush();
    }

    public void removeTopicFromProject(Project project, String topicName)
            throws AppException {
        ProjectTopics pt = em.find(ProjectTopics.class, 
                new ProjectTopicsPK(topicName, project.getId()));
        
        if (pt != null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Kafka topic does not exist in database.");
        }
        
        //remove topic from kafka cluster if project owns topic
        if (pt.getOwner()) {
            //remove from database
            pt = new ProjectTopics(topicName, project.getId(), pt.getOwner());
            em.remove(pt);
            //remove from zookeeper
            ZkClient zkClient = new ZkClient(getIp(settings.getZkIp()).getHostName(),
                    10 * 1000, 29 * 1000, ZKStringSerializer$.MODULE$);
            ZkConnection zkConnection = new ZkConnection(settings.getZkIp());
            ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);

            try {
                AdminUtils.deleteTopic(zkUtils, topicName);
            } catch (TopicExistsException ex) {
                throw new AppException(Response.Status.FOUND.getStatusCode(),
                        "Kafka topic cannot be removed from Kafka.");
            } finally {
                zkClient.close();
            }
        }
    }
    
    public void shareTopicToProject(String topicName, Project project) 
            throws AppException {

        ProjectTopics pt = em.find(ProjectTopics.class,
                new ProjectTopicsPK(topicName, project.getId()));
        
        if (pt != null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Kafka topic does not exist in database.");
        }
        //persist shared topic to database
        pt = new ProjectTopics(topicName, project.getId(), false);
        em.merge(pt);
        em.persist(pt);
        em.flush();
    }
    
    public void removeSharedTopicFromProject(String topicName, Project project)
            throws AppException{
    
     ProjectTopics pt = em.find(ProjectTopics.class, 
             new ProjectTopicsPK(topicName, project.getId()));
        if (pt != null
                ) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Kafka topic does not exist in database.");
        }
        pt = new ProjectTopics(topicName, project.getId(), pt.getOwner());
        em.remove(pt);
    }
     
    public void addAclsToTopic(String topicName, String projectName, AclDTO dto) throws AppException {
        addAclsToTopic(topicName, dto.getUsername(), projectName, dto.getPermissionType(), 
            dto.getOperationType(), dto.getHost(), dto.getRole(), dto.getShared());
    }
    private void addAclsToTopic(String topicName, String userName,
            String projectName, String permission_type, String operation_type,
            String host, String role, String shared) throws AppException {
       
        //get the project id
        TypedQuery<Project> query = em.createNamedQuery("Project.findByName",
                Project.class);
        query.setParameter("projectname", projectName);
        Project project  = query.getSingleResult();
        
        if (project == null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "The specified project for the topic is not in database");
        }
        
        ProjectTopics pt = em.find(ProjectTopics.class, 
                new ProjectTopicsPK(topicName, project.getId()));

        if (pt == null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Topic does not belong to the project.");
        }

        TopicAcls ta = new TopicAcls(pt.getId(), projectName+"__"+userName,
                permission_type, operation_type, host, role, shared);
        em.merge(ta);
        em.persist(ta);
        em.flush();

    }

    public void removeAclsFromTopic(String topicName, String userName,
            String projectName, String permission_type, String operation_type, 
            String host, String role, String shared) throws AppException {
        
        //get the project id
        TypedQuery<Project> query = em.createNamedQuery("Project.findByName",
                Project.class);
        query.setParameter("projectname", projectName);
        Project project  = query.getSingleResult();
        
        if (project == null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "The specified project for the topic is not in database");
        }
        
        ProjectTopics pt = em.find(ProjectTopics.class, 
                new ProjectTopicsPK(topicName, project.getId()));

        if (pt == null) {
            throw new AppException(Response.Status.FOUND.getStatusCode(),
                    "Topic does not belong to the project.");
        }

        
        TopicAcls acl = em.find(TopicAcls.class, 
                new TopicAclsPK(pt.getId(), projectName+"__"+userName,
                permission_type, operation_type, host, role));
        
        TopicAcls ta= new TopicAcls(pt.getId(), projectName+"__"+userName,
                permission_type, operation_type, host, role, acl.getShared());
        em.remove(ta);
    }    
    
    public Set<String> getBrokerList() throws AppException {

        int sessionTimeoutMs = 10 * 1000;//10 seconds
        Set<String> brokerList = new HashSet<>();

        try {
            ZooKeeper zk = new ZooKeeper("10.0.2.15:2181", sessionTimeoutMs, null);
            List<String> ids = zk.getChildren("/brokers/ids", false);
            for (String id : ids) {
                String brokerInfo = new String(zk.getData("/brokers/ids/" + id,
                        false, null));
                String delim = "[\"]";
                String[] tokens = brokerInfo.split(delim);
                for (String str : tokens) {
                    if (str.contains("//")) {
                        brokerList.add(str);
                    }
                }
            }
        } catch (IOException ex) {
           throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                   "Unable to find the zookeeper server");
        } catch (KeeperException | InterruptedException ex) { 
           throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                   "Unable to retrieve seed brokers from the kafka cluster.");
        }
        System.out.println(brokerList);
        return brokerList;
    }

    private Set<String> getTopicList() throws Exception {

        for (String seed : zkBrokerList) {
            kafka.javaapi.consumer.SimpleConsumer simpleConsumer = null;
            try {
                simpleConsumer = new SimpleConsumer(getIp(seed).getHostAddress(),
                        getPort(seed), 10 * 1000, 20 * 1000, "list_topics");

                //add ssl certificate to the consumer here
                List<String> topics = new ArrayList<>();

                TopicMetadataRequest req = new TopicMetadataRequest(topics);
                kafka.javaapi.TopicMetadataResponse resp = simpleConsumer.send(req);
                List<kafka.javaapi.TopicMetadata> topicMetadata = resp.topicsMetadata();
               
                for (kafka.javaapi.TopicMetadata metadata : topicMetadata) {
                    topicList.add(metadata.topic());
                }

            } catch (Exception ex) {
                throw new Exception("Error communicating to broker: " + seed);
            } finally {
                if (simpleConsumer != null) {
                    simpleConsumer.close();
                }
            }
        }
        
        return topicList;
    }
    
    private TopicDetailDTO getTopicDetailsfromKafkaCluster(String topicName)
            throws Exception {

        Map<Integer, Set<String>> replicas = new HashMap<>();
        Map<Integer, Set<String>> inSync = new HashMap<>();
        Map<Integer, String> partitionLeaders = new HashMap<>();

        for (String seed : zkBrokerList) {
            kafka.javaapi.consumer.SimpleConsumer simpleConsumer = null;            
            try {
                simpleConsumer = new SimpleConsumer(getIp(seed).getHostName(),
                        getPort(seed), 10 * 1000, 20 * 1000, "topic_detail");

                //add ssl certificate to the consumer here
                List<String> topics = new ArrayList<>();
                topics.add(topicName);

                TopicMetadataRequest req = new TopicMetadataRequest(topics);
                kafka.javaapi.TopicMetadataResponse resp = simpleConsumer.send(req);
                List<kafka.javaapi.TopicMetadata> topicsMetadata = resp.topicsMetadata();

                for (kafka.javaapi.TopicMetadata metadata : topicsMetadata) {

                    for (kafka.javaapi.PartitionMetadata partitionMetadata : metadata.partitionsMetadata()) {
                        int partId = partitionMetadata.partitionId();

                        //list the leaders of each parition
                        partitionLeaders.put(partId, partitionMetadata.leader().host());

                        //list the replicas of the parition
                        replicas.put(partId, new HashSet<String>());
                        for (kafka.cluster.BrokerEndPoint broker : partitionMetadata.replicas()) {
                            replicas.get(partId).add(broker.host());
                        }

                        //list the insync replicas of the parition
                        inSync.put(partId, new HashSet<String>());
                        for (kafka.cluster.BrokerEndPoint broker : partitionMetadata.isr()) {
                            inSync.get(partId).add(broker.host());
                        }
                    }
                }
            } catch (Exception ex) {
                throw new Exception("Error communicating to broker: " + seed,ex);
            } finally {
                if (simpleConsumer != null) {
                    simpleConsumer.close();
                }
            }
            System.out.println(topicList);
        }
        
        return new TopicDetailDTO(topicName, replicas, partitionLeaders, replicas);
    }
}
