package io.hops.hopsworks.common.dao.user.security.audit;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.userlogins")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Userlogins.findAll",
          query = "SELECT u FROM Userlogins u"),
  @NamedQuery(name = "Userlogins.findByLoginId",
          query = "SELECT u FROM Userlogins u WHERE u.loginId = :loginId"),
  @NamedQuery(name = "Userlogins.findByIp",
          query = "SELECT u FROM Userlogins u WHERE u.ip = :ip"),
  @NamedQuery(name = "Userlogins.findByOs",
          query = "SELECT u FROM Userlogins u WHERE u.os = :os"),
  @NamedQuery(name = "Userlogins.findByBrowser",
          query = "SELECT u FROM Userlogins u WHERE u.browser = :browser"),
  @NamedQuery(name = "Userlogins.findByAction",
          query = "SELECT u FROM Userlogins u WHERE u.action = :action"),
  @NamedQuery(name = "Userlogins.findByOutcome",
          query = "SELECT u FROM Userlogins u WHERE u.outcome = :outcome"),
  @NamedQuery(name = "Userlogins.findByUid",
          query = "SELECT u FROM Userlogins u WHERE u.uid = :uid"),
  @NamedQuery(name = "Userlogins.findByEmail",
          query = "SELECT u FROM Userlogins u WHERE u.email = :email"),
  @NamedQuery(name = "Userlogins.findByMac",
          query = "SELECT u FROM Userlogins u WHERE u.mac = :mac"),
  @NamedQuery(name = "Userlogins.findByLoginDate",
          query = "SELECT u FROM Userlogins u WHERE u.loginDate = :loginDate")})
public class Userlogins implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "login_id")
  private Long loginId;
  @Size(max = 16)
  @Column(name = "ip")
  private String ip;
  @Size(max = 30)
  @Column(name = "os")
  private String os;
  @Size(max = 40)
  @Column(name = "browser")
  private String browser;
  @Size(max = 45)
  @Column(name = "mac")
  private String mac;
  @Size(max = 254)
  @Column(name = "email")
  private String email;
  @Size(max = 80)
  @Column(name = "action")
  private String action;
  @Size(max = 20)
  @Column(name = "outcome")
  private String outcome;
  @Column(name = "uid")
  private Integer uid;
  @Column(name = "login_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date loginDate;

  public Userlogins() {
  }

  public Userlogins(Long loginId) {
    this.loginId = loginId;
  }

  public Long getLoginId() {
    return loginId;
  }

  public void setLoginId(Long loginId) {
    this.loginId = loginId;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getOs() {
    return os;
  }

  public void setOs(String os) {
    this.os = os;
  }

  public String getBrowser() {
    return browser;
  }

  public void setBrowser(String browser) {
    this.browser = browser;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public Integer getLastUserLoginUid() {
    return uid;
  }

  public void setUid(Integer uid) {
    this.uid = uid;
  }

  public Date getLoginDate() {
    return loginDate;
  }

  public void setLoginDate(Date loginDate) {
    this.loginDate = loginDate;
  }

  public Integer getUid() {
    return uid;
  }

  public String getMac() {
    return mac;
  }

  public void setMac(String mac) {
    this.mac = mac;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (loginId != null ? loginId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Userlogins)) {
      return false;
    }
    Userlogins other = (Userlogins) object;
    if ((this.loginId == null && other.loginId != null) || (this.loginId != null
            && !this.loginId.equals(other.loginId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.Userlogins[ loginId=" + loginId + " ]";
  }

}
