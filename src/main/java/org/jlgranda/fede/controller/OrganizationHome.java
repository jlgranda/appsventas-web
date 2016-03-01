/*
 * Copyright (C) 2016 jlgranda
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

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.OrganizationService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.model.management.Organization;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.Membership;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@RequestScoped
public class OrganizationHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(BalancedScoreCardHome.class);
    
    Organization organization;
    
    @EJB
    OrganizationService organizationService;
    
    @Inject
    private SettingHome settingHome;
    
    @Inject
    @LoggedIn
    private Subject subject;
    
    private TreeNode organizationNode;
    private TreeNode selectedNode;
    private TreeNode rootNode;
    private boolean toHaveChildren;

    @PostConstruct
    private void init() {
        setOrganization(organizationService.createInstance());
    }
    
    @Override
    public void handleReturn(SelectEvent event) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public boolean mostrarFormularioOrganizacion() {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "550");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "480");
        super.openDialog(SettingNames.POPUP_FORMULARIO_ORGANIZATION, width, height, true);
        return true;
    }
    
    public void save(){
        getOrganization().setAuthor(subject);
        getOrganization().setOwner(subject);
        organizationService.save(getOrganization().getId(), getOrganization());
        addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully.detail"));
    }
    
    public List<Organization> find(Subject subject){
        return organizationService.findByNamedQuery("Organization.findByOwner", subject);
    }
    
    public List<Organization> find(){
        return find(subject);
    }
    
    public BigDecimal countRowsByTag(String tag) {
        BigDecimal total = new BigDecimal(0);
        if ("all".equalsIgnoreCase(tag)){
            total = new BigDecimal(organizationService.count());
        } else if ("own".equalsIgnoreCase(tag)){
            total = new BigDecimal(organizationService.count("Organization.countByOwner", subject));
        } else {
            total = new BigDecimal(organizationService.count("Organization.countByOwnerAndOrganization", subject, getOrganization()));
        }
        return total;
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public boolean getToHaveChildren(TreeNode node) {
        if (node.getChildCount() == 0) {
            toHaveChildren = false;
        } else {
            toHaveChildren = true;
        }
        return toHaveChildren;
    }

    public void setToHaveChildren(boolean toHaveChildren) {
        this.toHaveChildren = toHaveChildren;
    }
    
    public TreeNode getOrganizationNode() {
        if (organizationNode == null) {
            buildTree();
        }
        return organizationNode;
    }

    public void setOrganizationNode(TreeNode organizationNode) {
        this.organizationNode = organizationNode;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public TreeNode getRootNode() {
        if (rootNode == null || organizationNode.getChildCount() == 0) {
            buildTree();
        }
        return rootNode;

    }

    public void setRootNode(TreeNode rootNode) {
        this.rootNode = rootNode;
    }
    
    public TreeNode buildTree() {
        rootNode = new DefaultTreeNode("rootNode", "", null);
        organizationNode = new DefaultTreeNode("organization", getOrganization(), rootNode);

        rootNode.setExpanded(true);
        organizationNode.setExpanded(true);

        TreeNode macroprocessNode = null;
        TreeNode processNode = null;
        TreeNode themeNode = null;
        TreeNode ownerNode = null;

        for (Membership m : getOrganization().getMemberships()) {
            ownerNode = new DefaultTreeNode("group", m, organizationNode);
            ownerNode.setExpanded(true);
            
        }
        return rootNode;
    }
    
    public void onNodeSelect(NodeSelectEvent event) {
        BussinesEntity entity = (BussinesEntity) event.getTreeNode().getData();
        
    }
}
