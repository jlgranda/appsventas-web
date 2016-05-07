/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlgranda.fede.security;

import com.jlgranda.fede.ejb.SubjectService;
import javax.ejb.EJB;
import org.picketlink.annotations.PicketLink;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.jpapi.model.profile.Subject;
import org.picketlink.Identity;
import org.picketlink.idm.model.Account;

/**
 * Recursos transversales del sistema
 * @author jlgranda
 */
public class Resources {
    
    @EJB
    SubjectService subjectService;
    
    @SuppressWarnings("unused")
    @Produces
    @PicketLink
    @PersistenceContext(unitName = "fede")
    private static EntityManager picketLinkEntityManager;
    
    
    @Produces
    @Named("subject")
    public Subject getLoggedIn(Identity identity) {
        Subject loggedIn = null;
        if (identity.isLoggedIn()) {
            try {
                Account account = identity.getAccount();
                
                loggedIn = subjectService.findUniqueByNamedQuery("Subject.findUserByUUID", account.getId());
                
                if (loggedIn != null) {
                    loggedIn.setLoggedIn(true);
                }
            } catch (NoResultException e) {
                throw e;
            }
        } 
        return loggedIn;
    }
}