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
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.OrganizationService;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Record;
import org.jpapi.model.Organization;
import org.jlgranda.fede.ui.model.LazyOrganizationDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class OrganizationHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -7712000810185595430L;

    Logger logger = LoggerFactory.getLogger(OrganizationHome.class);
    
    Long organizationId;
    
    Organization organization;
    
    @EJB
    OrganizationService organizationService;
    
    @Inject
    private SettingHome settingHome;
    
    private Subject subject;
    
    private TreeNode organizationNode;
    private TreeNode selectedNode;
    private TreeNode rootNode;
    private boolean toHaveChildren;
    
    private LazyOrganizationDataModel lazyDataModel;
    
    @EJB
    private GroupService groupService;

    @PostConstruct
    private void init() {
        setOrganization(organizationService.createInstance());
        setOutcome("organizations");
    }
    
    @Override
    public void handleReturn(SelectEvent event) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Organization getOrganization() {
        if (this.organizationId != null && !this.organization.isPersistent()) {
            this.organization = organizationService.find(this.organizationId);
        }
        return this.organization;
    }
    
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public LazyOrganizationDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyOrganizationDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public boolean mostrarFormularioOrganizacion() {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "550");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "480");
        super.openDialog(SettingNames.POPUP_FORMULARIO_ORGANIZATION, width, height, true);
        return true;
    }
    
    public void save(){
        
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println("id: " + getOrganization().getId());
        System.out.println("name: " + getOrganization().getName());
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        
        if (!getOrganization().isPersistent()){
            getOrganization().setAuthor(subject);
            getOrganization().setOwner(subject);
        }
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
        return null;
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

//        TreeNode macroprocessNode = null;
//        TreeNode processNode = null;
//        TreeNode themeNode = null;
//        TreeNode ownerNode = null;
//
//        for (Membership m : getOrganization().getMemberships()) {
//            ownerNode = new DefaultTreeNode("group", m, organizationNode);
//            ownerNode.setExpanded(true);
//            
//        }
        return rootNode;
    }
    
    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyOrganizationDataModel(organizationService);
        }

        //lazyDataModel.setOwner(subject);
        lazyDataModel.setAuthor(subject);
        lazyDataModel.setStart(getStart());
        lazyDataModel.setEnd(getEnd());

        if (getKeyword() != null && getKeyword().startsWith("label:")) {
            String parts[] = getKeyword().split(":");
            if (parts.length > 1) {
                lazyDataModel.setTags(parts[1]);
            }
            lazyDataModel.setFilterValue(null);//No buscar por keyword
        } else {
            lazyDataModel.setTags(getTags());
            lazyDataModel.setFilterValue(getKeyword());
        }
    }

    
    @Override
    public List<org.jpapi.model.Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "security", "security"), org.jpapi.model.Group.Type.LABEL));
        }

        return this.groups;
    }
    
    public void onRowSelect(SelectEvent event) {
        try {
            //TODO establecer objeto en edición y levantar popup
            if (event != null && event.getObject() != null) {
                redirectTo("/pages/management/organization/organization.jsf?organizationId=" + ((Organization) event.getObject()).getId() + "&faces-redirect=true");
            }
        } catch (IOException ex) {
            //java.util.logging.Logger.getLogger(org.picketlink.idm.model.basic.Group.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void onRowUnselect(UnselectEvent event) {
        FacesMessage msg = new FacesMessage(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());
        FacesContext.getCurrentInstance().addMessage(null, msg);
        logger.info(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public List<Organization> findOrganizations(){
        return organizationService.getOrganizations();
    }
    
    public List<Organization> findOrganizations(Subject subject) {
        return organizationService.findByNamedQuery("Organization.findByEmployee", subject);
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Ir al inicio
     */
    public void irAInicio(){
        setOutcome("");
        try {
            this.redirectTo(getOutcome());
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(OrganizationHome.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
