/*
 * Copyright (C) 2015 jlgranda
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.jlgranda.fede.controller;

import com.jlgranda.fede.ejb.SettingService;
import com.jlgranda.fede.ejb.SubjectService;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jpapi.model.CodeType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.jpapi.util.Strings;
import org.picketlink.Identity;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.model.basic.BasicModel;
import static org.picketlink.idm.model.basic.BasicModel.addToGroup;
import static org.picketlink.idm.model.basic.BasicModel.grantGroupRole;
import static org.picketlink.idm.model.basic.BasicModel.grantRole;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador de entidades Subject: signup, profile
 *
 * @author jlgranda
 */
@Named
@RequestScoped
public class SubjectHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -1007161141552849702L;

    Logger logger = LoggerFactory.getLogger(SubjectHome.class);

    @Inject
    private Identity identity;

    Subject loggedIn = new Subject();

    Subject signup = null;

    @EJB
    SubjectService subjectService;

    @EJB
    SettingService settingService;

    @Inject
    GroupHome groupHome;
    @Inject
    private PartitionManager partitionManager;

    @Resource
    private UserTransaction userTransaction; //https://issues.jboss.org/browse/PLINK-332

    IdentityManager identityManager = null;

    @Produces
    @LoggedIn
    @Named("subject")
    public Subject getLoggedIn() {
        if (identity.isLoggedIn() && !loggedIn.isPersistent()) {
            try {
                Account account = identity.getAccount();
                loggedIn = subjectService.findUniqueByNamedQuery("Subject.findUserByUUID", account.getId());
                if (loggedIn != null) {
                    loggedIn.setLoggedIn(true);
                }
            } catch (NoResultException e) {
                throw e;
            }
        } else if (!identity.isLoggedIn()) {
        }
        return loggedIn;
    }

    public boolean isLoggedIn() {
        return loggedIn != null && loggedIn.getId() != null;
    }

    public void save(Subject subject) {
        subjectService.save(subject.getId(), subject);
        addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully.detail"));
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Subject getSignup() {
        if (signup == null) {
            signup = subjectService.createInstance();
        }
        return signup;
    }

    public void setSignup(Subject signup) {
        this.signup = signup;
    }

    /**
     * Procesa la creación de una cuenta en fede para el usuario dado
     * @param _signup el objeto <tt>Subject</tt> a agregar
     * @param owner el propietario del objeto a agregar
     */
    public void processSignup(Subject _signup, Subject owner) {

        identityManager = partitionManager.createIdentityManager();

        logger.info("Procesar signup para {} ", _signup);
        if (_signup != null) {
            //Crear la identidad para acceso al sistema
            try {

                //Prepare password
                Password password = new Password(_signup.getPassword());
                //separar nombres
                List<String> names = Strings.splitNamesAt(_signup.getFirstname());

                if (names.size() > 1) {
                    _signup.setFirstname(names.get(0));
                    _signup.setSurname(names.get(1));
                }
                _signup.setUsername(_signup.getEmail());

                this.userTransaction.begin();
                User user = new User(_signup.getUsername());
                user.setFirstName(_signup.getFirstname());
                user.setLastName(_signup.getSurname());
                user.setEmail(_signup.getEmail());
                user.setCreatedDate(Dates.now());
                identityManager.add(user);

                identityManager.updateCredential(user, password);

                // Create application role "superuser"
                Role superuser = BasicModel.getRole(identityManager, "superuser");

                Group group = BasicModel.getGroup(identityManager, "fede");

                RelationshipManager relationshipManager = partitionManager.createRelationshipManager();
                // Make john a member of the "sales" group
                addToGroup(relationshipManager, user, group);
                // Make mary a manager of the "sales" group
                grantGroupRole(relationshipManager, user, superuser, group);
                // Grant the "superuser" application role to jane
                grantRole(relationshipManager, user, superuser);

                this.userTransaction.commit();

                //Conectar con el user auth
                String passwrod_ = new BasicPasswordEncryptor().encryptPassword(new String(password.getValue()));
                _signup.setUsername(_signup.getEmail());
                _signup.setCodeType(CodeType.CEDULA);
                _signup.setPassword(passwrod_);
                _signup.setUsernameConfirmed(true);

                //Set fede email
                _signup.setFedeEmail(_signup.getCode().concat("@").concat(settingService.findByName("mail.imap.host").getValue()));
                _signup.setFedeEmailPassword(passwrod_);

                //Finalmente crear en fede
                _signup.setUuid(user.getId());
                _signup.setSubjectType(Subject.Type.NATURAL);
                _signup.setOwner(owner);
                subjectService.save(_signup);

                //Crear grupos por defecto para el subject
                groupHome.createDefaultGroups(_signup);

            } catch (NotSupportedException | SystemException | IdentityManagementException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException e) {
                try {
                    this.userTransaction.rollback();
                } catch (SystemException ignore) {
                }
                throw new RuntimeException("Could not create default security entities.", e);
            }
        }

    }
    
    /**
     * Procesa la creación de una cuenta en fede
     */
    public void processSignup() {
        processSignup(this.signup, null);
    }

    public List<Subject> find(String keyword) {
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> columns = new HashMap<>();
        columns.put("username", keyword);
        columns.put("firstname", keyword);
        columns.put("surname", keyword);
        filters.put("dummy", columns);
        QueryData<Subject> queryData = subjectService.find(-1, -1, "surname, firstname", QuerySortOrder.ASC, filters);
        return queryData.getResult();
    }

    @Override
    public org.jpapi.model.Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
