/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.security.ua.model.People;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class PeopleStatusBean implements Serializable{

    private static final Logger logger = Logger.getLogger(UserManager.class.getName());

    private boolean open_reauests = false;
    
    private int tabIndex;

    @EJB
    private UserManager userManager;

    private People user;

    public boolean isOpen_reauests() {
        return checkForRequests();
    }

    public void setOpen_reauests(boolean open_reauests) {
        this.open_reauests = open_reauests;
    }

    public People getUser() {
        if (user == null) {
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            String userEmail = context.getUserPrincipal().getName();
            user = userManager.findByEmail(userEmail);
        }
        return user;
    }

    public void setTabIndex(int index) {
        this.tabIndex = index;
    }

    public int getTabIndex() {
        int oldindex = tabIndex;
        tabIndex = 0;
        return oldindex;
    }


    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }

    
    /**
     * Return systemwide admin for user administration 
     * @return 
     */
    public boolean isSYSAdmin() {
        People p = userManager.findByEmail(getRequest().getRemoteUser());
        return userManager.findGroups(p.getUid()).contains("SYS_ADMIN");
    }
    
    /**
     * Return both system wide and study wide roles
     * @return 
     */
    public boolean isAdminUser(){
        People p = userManager.findByEmail(getRequest().getRemoteUser());
        List<String> roles = userManager.findGroups(p.getUid());
            
        return (roles.contains("SYS_ADMIN") || roles.contains("BBC_ADMIN"));
    }

   
    /**
     * Return study owner role
     * @return 
     */
    public boolean isBBCAdmin() {
        People p = userManager.findByEmail(getRequest().getRemoteUser());
        return userManager.findGroups(p.getUid()).contains("BBC_ADMIN");    
    }

    
    /**
     * Return study members role
     * @return 
     */
    
    public boolean isAnyAuthorizedRole() {

        People p = userManager.findByEmail(getRequest().getRemoteUser());
        List<String> roles = userManager.findGroups(p.getUid());
        return  (roles.contains("SYS_ADMIN") || roles.contains("BBC_ADMIN") || roles.contains("BBC_USER")|| roles.contains("BBC_RESEARCHER") );
     }  
    
    
    /**
     * 
     * @return 
     */
     public boolean checkForRequests() {
        if (isAdminUser()) {
            //return false if no requests
            open_reauests = !(userManager.findAllByStatus(AccountStatusIF.MOBILE_ACCOUNT_INACTIVE).isEmpty()) || !(userManager.findAllByStatus(AccountStatusIF.YUBIKEY_ACCOUNT_INACTIVE).isEmpty());
        }
      return open_reauests;
    }


    public String logOut() {
        getRequest().getSession().invalidate();
        return "welcome";
    }
    
    public boolean isLoggedIn(){
        return getRequest().getRemoteUser() != null;
    }

    public String openRequests() {
        this.tabIndex = 1;
        return "userMgmt";
    }

}
