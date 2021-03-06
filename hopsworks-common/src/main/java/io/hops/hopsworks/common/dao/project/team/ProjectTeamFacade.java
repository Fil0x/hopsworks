package io.hops.hopsworks.common.dao.project.team;

import io.hops.hopsworks.common.dao.project.Project;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.user.Users;

@Stateless
public class ProjectTeamFacade {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  protected EntityManager getEntityManager() {
    return em;
  }

  public ProjectTeamFacade() {
  }

  /**
   * Count the number of members in this project with the given role.
   * <p/>
   * @param project
   * @param role
   * @return
   */
  public int countProjectTeam(Project project, String role) {
    TypedQuery<Long> query = em.createNamedQuery(
            "ProjectTeam.countMembersForProjectAndRole", Long.class);
    query.setParameter("project", project);
    query.setParameter("teamRole", role);
    return query.getSingleResult().intValue();
  }

  /**
   * Count the number of members in this project.
   * <p/>
   * @param project
   * @return
   */
  public int countMembersInProject(Project project) {
    TypedQuery<Long> query = em.createNamedQuery(
            "ProjectTeam.countAllMembersForProject", Long.class);
    query.setParameter("project", project);
    return query.getSingleResult().intValue();
  }

  /**
   * Find all team members in project 'name' with role 'role'.
   * <p/>
   * @param project
   * @param role
   * @return A list of Users entities that have the role <i>role</i> in Project
   * <i>project</i>.
   */
  public List<Users> findTeamMembersByProject(Project project, String role) {
    List<ProjectTeam> results = findProjectTeamByProjectAndRole(project, role);
    ArrayList<Users> retList = new ArrayList<>(results.size());
    for (ProjectTeam st : results) {
      retList.add(st.getUser());
    }
    return retList;
  }

  /**
   * Get all the ProjectTeam entries for users in the role <i>role</i> in
   * Project <i>project</i>.
   * <p/>
   * @param project
   * @param role
   * @return
   */
  public List<ProjectTeam> findProjectTeamByProjectAndRole(Project project,
          String role) {
    TypedQuery<ProjectTeam> q = em.createNamedQuery(
            "ProjectTeam.findMembersByRoleInProject", ProjectTeam.class);
    q.setParameter("project", project);
    q.setParameter("teamRole", role);
    return q.getResultList();
  }

  /**
   * Find all the ProjectTeam entries for members in Project <i>project</i> that
   * have the role Researcher.
   * <p/>
   * @param project
   * @return
   */
  public List<ProjectTeam> findResearchMembersByName(Project project) {
    return findProjectTeamByProjectAndRole(project, "Researcher");
  }

  /**
   * Find all the ProjectTeam entries for members in Project <i>project</i> that
   * have the role Guest.
   * <p/>
   * @param project
   * @return
   */
  public List<ProjectTeam> findGuestMembersByName(Project project) {
    return findProjectTeamByProjectAndRole(project, "Guest");
  }

  /**
   * Get all the ProjectTeam entries for the given project.
   * <p/>
   * @param project
   * @return
   */
  public List<ProjectTeam> findMembersByProject(Project project) {
    TypedQuery<ProjectTeam> query = em.createNamedQuery(
            "ProjectTeam.findByProject",
            ProjectTeam.class);
    query.setParameter("project", project);
    return query.getResultList();
  }

  /**
   * Find all ProjectTeam entries containing the given Users as member.
   * <p/>
   * @param member
   * @return
   */
  public List<ProjectTeam> findActiveByMember(Users member) {
    Query query = em.createNamedQuery("ProjectTeam.findActiveByTeamMember",
            ProjectTeam.class).setParameter("user", member);
    List<ProjectTeam> x = query.getResultList();

    //return query.getResultList();
    return x;
  }

  /**
   * Count the number of studies this user is a member of.
   * <p/>
   * @param user
   * @return
   */
  public int countByMember(Users user) {
    TypedQuery<Long> query = em.createNamedQuery(
            "ProjectTeam.countStudiesByMember", Long.class);
    query.setParameter("user", user);
    return query.getSingleResult().intValue();
  }

  /**
   * Count the number of studies the user with this email address is a member
   * of.
   * <p/>
   * @param email
   * @return
   * @deprecated Use countByMember(User user) instead.
   */
  public int countByMemberEmail(String email) {
    TypedQuery<Users> query = em.createNamedQuery(
            "Users.findByEmail", Users.class);
    query.setParameter("email", email);
    Users user = query.getSingleResult();
    return countByMember(user);
  }

  /**
   * Get the current role of Users <i>user</i> in Project <i>project</i>.
   * <p/>
   * @param project
   * @param user
   * @return The current role of the user in the project, or null if the user is
   * not in it.
   */
  public String findCurrentRole(Project project, Users user) {
    TypedQuery<ProjectTeam> q = em.createNamedQuery(
            "ProjectTeam.findRoleForUserInProject", ProjectTeam.class);
    q.setParameter("project", project);
    q.setParameter("user", user);
    try {
      return q.getSingleResult().getTeamRole();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Get the current role of Users <i>user</i> in Project <i>project</i>.
   * <p/>
   * @param project
   * @param user
   * @return The current role of the user in the project, or null if the user is
   * not in it.
   * @deprecated use findCurrentRole(Project project, User user) instead.
   */
  public String findCurrentRole(Project project, String user) {
    TypedQuery<Users> q = em.createNamedQuery("Users.findByEmail", Users.class);
    q.setParameter("email", user);
    Users u = q.getSingleResult();
    return findCurrentRole(project, u);
  }

  public void persistProjectTeam(ProjectTeam team) {
    em.persist(team);
  }

  /*
   * merges an update to project team
   * @param team
   */
  public void update(ProjectTeam team) {
    if (team != null) {
      em.merge(team);
    }
  }

  /**
   * Remove the ProjectTeam entry for Users <i>user</i> in Project
   * <i>project</i>.
   * <p/>
   * @param project
   * @param user
   */
  public void removeProjectTeam(Project project, Users user) {
    ProjectTeam team = findByPrimaryKey(project, user);
    if (team != null) {
      em.remove(team);
    }
  }

  /**
   * Remove the ProjectTeam entry for the Users with email <i>email</i> in
   * Project <i>project</i>.
   * <p/>
   * @param project
   * @param email
   * @deprecated Use removeProjectTeam(Project project, User user) instead.
   */
  public void removeProjectTeam(Project project, String email) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByEmail",
            Users.class);
    query.setParameter("email", email);
    removeProjectTeam(project, query.getSingleResult());
  }

  /**
   * Update the team role of Users <i>user</i> in Project <i>project</i>.
   * <p/>
   * @param project
   * @param user
   * @param teamRole
   */
  public void updateTeamRole(Project project, Users user, String teamRole) {
    ProjectTeam team = findByPrimaryKey(project, user);
    if (team != null) {
      team.setTeamRole(teamRole);
      team.setTimestamp(new Date());
      em.merge(team);
    }
  }

  /**
   * Update the team role of the Users with email <i>email</i> in Project
   * <i>project</i>.
   * <p/>
   * @param project
   * @param email
   * @param teamRole
   */
  public void updateTeamRole(Project project, String email, String teamRole) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByEmail",
            Users.class);
    query.setParameter("email", email);
    updateTeamRole(project, query.getSingleResult(), teamRole);
  }

  /**
   * Update the team role of all users in Project <i>project</i>.
   * <p/>
   * @param project
   * @param teamRole
   */
  public List<ProjectTeam> updateTeamRole(Project project, ProjectRoleTypes teamRole) {
    List<ProjectTeam> teamMembers = findMembersByProject(project);
    for (ProjectTeam meberber : teamMembers) {
      meberber.setTeamRole(teamRole.getRole());
      meberber.setTimestamp(new Date());
      em.merge(meberber);
    }
    return teamMembers;
  }

  /**
   * Find the ProjectTeam entry for Project <i>project</i> and Users
   * <i>user</i>.
   * <p/>
   * @param project
   * @param user
   * @return
   */
  public ProjectTeam findByPrimaryKey(Project project, Users user) {
    return em.find(ProjectTeam.class, new ProjectTeam(project, user).
            getProjectTeamPK());
  }

  /**
   * Check if the Users <i>user</i> is a member of the Project <i>project</i>.
   * <p/>
   * @param project
   * @param user
   * @return
   */
  public boolean isUserMemberOfProject(Project project, Users user) {
    TypedQuery<ProjectTeam> q = em.createNamedQuery(
            "ProjectTeam.findRoleForUserInProject", ProjectTeam.class);
    q.setParameter("project", project);
    q.setParameter("user", user);
    return q.getResultList().size() > 0;
  }

  /**
   * Check if the Users <i>user</i> is a member of the Project <i>project</i>.
   * <p/>
   * @param project
   * @param user
   * @return
   * @deprecated use isUserMemberOfProject(Project project,User user) instead.
   */
  public boolean isUserMemberOfProject(Project project, String user) {
    TypedQuery<Users> q = em.createNamedQuery("Users.findByEmail", Users.class);
    q.setParameter("email", user);
    return isUserMemberOfProject(project, q.getSingleResult());
  }

  /**
   * Find the ProjectTeam entry for the given project and user.
   * <p/>
   * @param project
   * @param user
   * @return The ProjectTeam entry, null if it does not exist.
   */
  public ProjectTeam findProjectTeam(Project project, Users user) {
    TypedQuery<ProjectTeam> q = em.createNamedQuery(
            "ProjectTeam.findRoleForUserInProject",
            ProjectTeam.class);
    q.setParameter("user", user);
    q.setParameter("project", project);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public Users findUserByEmail(String userEmail) {
    TypedQuery<Users> q = em.createNamedQuery(
            "Users.findByEmail", Users.class);
    q.setParameter("email", userEmail);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
