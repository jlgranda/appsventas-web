/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlgranda.fede.security;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;
import org.apache.shiro.SecurityUtils;
import org.jlgranda.fede.cache.LoginSubjectCacheProvider;
import org.jpapi.model.profile.Subject;

/**
 * Recursos transversales del sistema
 * @author jlgranda
 */
public class Resources {
    
    //@EJB
    //SubjectService subjectService;
    
    @Inject
    LoginSubjectCacheProvider loginSubjectCacheProvider;
    
        
    @Produces
    @Named("subject")
    public Subject getLoggedIn() throws Exception {
        
        boolean isLoggedIn = true;
        org.apache.shiro.subject.Subject subject = SecurityUtils.getSubject();

        if (!subject.isAuthenticated()/* && hasAnnotation(c, m, RequiresAuthentication.class)*/) {
            isLoggedIn = false;
            //throw new UnauthenticatedException("Authentication required");
        }

        
        Subject loggedIn = null;
        if (isLoggedIn) {
            try {
                loggedIn = loginSubjectCacheProvider.getSubjectCache().get(subject.getPrincipal().toString());
                
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