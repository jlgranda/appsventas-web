/*
 * Copyright (C) 2022 usuario
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jlgranda.fede.controller.production;

import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.production.AggregationDetailService;
import com.jlgranda.fede.ejb.production.AggregationService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import java.awt.Event;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.math3.stat.interval.AgrestiCoullInterval;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationData;
import org.jlgranda.fede.model.accounting.Record;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.primefaces.event.SelectEvent;
import org.jlgranda.appsventas.data.AggregationData;
import org.jlgranda.fede.Constantes;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.production.Aggregation;
import org.jlgranda.fede.model.production.AggregationDetail;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.model.sales.ProductType;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author usuario
 */
@Named
@ViewScoped
public class AggregationHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(AggregationHome.class);

    @Inject
    private Subject subject;
    @Inject
    private OrganizationData organizationData;
    @Inject
    private SettingHome settingHome;

    @EJB
    private GroupService groupService;
    @EJB
    private AggregationService aggregationService;
    @EJB
    private AggregationDetailService aggregationDetailService;
    @EJB
    private ProductCache productCache;

    /**
     * EDIT OBJECT.
     */
    private Long aggregationId;
    private Aggregation aggregation;
    private AggregationDetail aggregationDetail;
    
    private List<AggregationDetail> candidateAggregationDetail = new ArrayList<>();

    /**
     * UX.
     */
    private List<AggregationData> aggregations;
    private List<AggregationDetail> selectedAggregationDetails = new ArrayList<>();

    @PostConstruct
    private void init() {
        setAggregation(aggregationService.createInstance());
        setAggregationDetail(aggregationDetailService.createInstance());
        setAggregations(aggregationService.buildDatafindByOrganization(this.organizationData.getOrganization()));
        initializeActions();
        setOutcome("aggregations");
    }

    public Long getAggregationId() {
        return aggregationId;
    }

    public void setAggregationId(Long aggregationId) {
        this.aggregationId = aggregationId;
    }

    public Aggregation getAggregation() {
        if (this.aggregationId != null && this.aggregation != null && !this.aggregation.isPersistent()) {
            this.aggregation = aggregationService.find(this.aggregationId);
        }
        return this.aggregation;
    }

    public void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
    }

    public AggregationDetail getAggregationDetail() {
        return this.aggregationDetail;
    }

    public void setAggregationDetail(AggregationDetail aggregationDetail) {
        this.aggregationDetail = aggregationDetail;
    }

    public List<AggregationData> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<AggregationData> aggregations) {
        this.aggregations = aggregations;
    }


    public List<AggregationDetail> getCandidateAggregationDetail() {
        return candidateAggregationDetail;
    }

    public void setCandidateAggregationDetail(List<AggregationDetail> candidateAggregationDetail) {
        this.candidateAggregationDetail = candidateAggregationDetail;
    }

    
    public List<AggregationDetail> getSelectedAggregationDetails() {
        return selectedAggregationDetails;
    }

    public void setSelectedAggregationDetails(List<AggregationDetail> selectedAggregationDetails) {
        this.selectedAggregationDetails = selectedAggregationDetails;
    }

    @Override
    public List<SelectItem> getActions() {
        return actions;
    }

    @Override
    public void setActions(List<SelectItem> actions) {
        this.actions = actions;
    }

    /**
     * METHODS.
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public boolean addAggregationDetail() throws IllegalAccessException, InvocationTargetException {
        if (this.aggregationDetail.getProduct() == null) {
            addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("common.requiredMessage"));
            logger.error(I18nUtil.getMessages("common.selected.product.none"));
            return false;
        }
        
        if ( this.aggregationDetail.getQuantity() != null 
                && this.aggregationDetail.getPriceUnit() != null ) {

            if (BigDecimal.ZERO.compareTo(this.aggregationDetail.getQuantity()) == -1 && BigDecimal.ZERO.compareTo(this.aggregationDetail.getPriceUnit()) == -1) {
                
                AggregationDetail detail =  aggregationDetailService.createInstance();
                detail.setProductId(this.aggregationDetail.getProduct().getId());
                detail.setProduct(this.aggregationDetail.getProduct()); 
                detail.setQuantity(this.aggregationDetail.getQuantity());
                detail.setPriceUnit(this.aggregationDetail.getPriceUnit());
                detail.setCost(this.aggregationDetail.getQuantity().multiply(this.aggregationDetail.getPriceUnit()));
                
                this.aggregation.addAggregationDetail(detail); //Agrega a la lista de detalles

                this.aggregationDetail = aggregationDetailService.createInstance(); //Listo para nueva selección
//                    if (!this.aggregation.isPersistent()) {//Guardar primero la agregación, si esta es nula
//                        aggregationService.save(aggregation);
//                    } else {
//                        aggregationService.save(aggregation.getId(), aggregation);
//                    }
                //saveAggregationDetailValid();
            }
        } else {
            addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("property.required.invalid.form"));
        }
        return true;
    }
    
    public void editAggregationDetail(AggregationDetail detail){
        this.aggregationDetail = detail; //En edición
    }
    
    /**
     * Marcar como borrado
     * @param detail 
     */
    public void deleteAggregationDetail(AggregationDetail detail){
        
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<< !!!" + detail);
         
        detail.setDeleted(Boolean.TRUE);
        detail.setName(Strings.toUpperCase(Constantes.ESTADO_ELIMINADO + Constantes.SEPARADOR + detail.getProduct().getName())); //Registrar el producto que estaba referenciado
        detail.setDescription(detail.getProduct().toString()); //Registrar el producto que estaba referenciado
        detail.setProduct(null);
//                    aggregationDetailService.save(instance.getId(), instance); //Actualizar la entidad
//                    aggregation.removeAggregationDetail(instance); //Quitar de la vista
        this.aggregation.addAggregationDetail(detail); //Reemplaza el detalle con el nuevo valor modificado para borrar
    }

    public void saveAggregation() {
       
        if (this.aggregation.isPersistent()) {
            
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<");
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<");
            System.out.println("GUARDANDO OBJETO PERSISTENTE: " + this.aggregation.getId());
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<");
            //Recargar el objecto para actualizar
            this.aggregation.setLastUpdate(Dates.now());
            aggregationService.save(this.aggregation.getId(), this.aggregation);
        } else {
            this.aggregation.setAuthor(subject);
            this.aggregation.setOwner(subject);
            this.aggregation.setOrganization(this.organizationData.getOrganization());
            this.aggregation.setName(this.aggregation.getProduct().getName());
            //Remover la referencia a dettached entity
            this.aggregation.getAggregationDetails().forEach(agd -> {
                agd.setProduct(null);
            });
            aggregationService.save(this.aggregation);
        }
        
        //setAggregationId(this.aggregation.getId());
        //getAggregation();
    }

    private void saveAggregationDetailValid() {
        if (this.aggregationDetail.isPersistent()) {
            this.aggregationDetail.setLastUpdate(Dates.now());
        } else {
            this.aggregationDetail.setAuthor(subject);
            this.aggregationDetail.setOwner(subject);
            this.aggregationDetail.setAggregation(this.aggregation);
        }
        aggregationDetailService.save(this.aggregationDetail.getId(), this.aggregationDetail);
        this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully.detail"), String.valueOf(this.aggregationDetail.getProduct().getName()));
        getAggregation();
        //Encerar el registro
        this.aggregationDetail = aggregationDetailService.createInstance();
    }

    /**
     * METHODS UTIL.
     */
    public List<Product> filterProductsService(String query) {
        return productCache.lookup(query, ProductType.SERVICE, this.organizationData.getOrganization()); //sólo servicios
    }

    public List<Product> filterProductsRawMaterial(String query) {
        return productCache.lookup(query, ProductType.RAW_MATERIAL, this.organizationData.getOrganization()); //sólo servicios
    }

    private void aggregationDetailValid() {
        if (this.aggregation.getId() == null) {//Guardar primero la agregación
            saveAggregation();
        }
        this.aggregationDetail.setOwner(this.subject);
        addAggregationDetail(this.aggregationDetail);
        saveAggregation();

        this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully.detail"), String.valueOf(this.aggregationDetail.getProduct().getName()));

        //Encerar el registro
        this.aggregationDetail = aggregationDetailService.createInstance();
    }

    private void addAggregationDetail(AggregationDetail aggd) {
        if (aggd.getId() != null) {
            this.aggregation.addAggregationDetail(aggd);
        } else {
            this.aggregation.addAggregationDetail(replaceAggregationDetail(this.aggregation.getAggregationDetails(), aggd));
        }
    }

    private AggregationDetail replaceAggregationDetail(List<AggregationDetail> list, AggregationDetail aggd) {
        AggregationDetail aggdAux = this.aggregation.getAggregationDetails().stream()
                .filter(d -> aggregationDetail.getProduct().getId().equals(d.getProduct().getId()))
                .findAny()
                .orElse(null);
        if (aggdAux != null && aggdAux.getId() != null) {
            aggdAux.setQuantity(aggd.getQuantity());
            aggdAux.setPriceUnit(aggd.getPriceUnit());
            aggdAux.setCost(aggd.getCost());
            return aggdAux;
        } else {
            return aggd;
        }
    }

    public void calculePriceUnit() {
        if (BigDecimal.ZERO.compareTo(this.aggregationDetail.getProduct().getPriceCost()) == -1) {
            this.aggregationDetail.setPriceUnit(this.aggregationDetail.getProduct().getPriceCost());
        }
    }

    public void onItemAggregation(AggregationData agg) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (agg != null && agg.getId() != null) {
                redirectTo("/pages/production/aggregation.jsf?aggregationId=" + agg.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntitsy"), (aggregationId));
        }
    }

    public void onRowSelectAggregationDetail(SelectEvent<AggregationDetail> event) {
    }

    private void initializeActions() {
        this.actions = new ArrayList<>();
//        SelectItem item = null;
//        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
//        actions.add(item);
//
//        item = new SelectItem("borrar", I18nUtil.getMessages("common.delete"));
//        actions.add(item);
    }

    public boolean isActionExecutable() {
        if ("borrar".equalsIgnoreCase(this.selectedAction) && !getSelectedAggregationDetails().isEmpty()) {
            return true;
        }
        return false;
    }

    public void execute() {
        if (this.isActionExecutable()) {
            if ("borrar".equalsIgnoreCase(this.selectedAction)) {
                for (AggregationDetail instance : getSelectedAggregationDetails()) {
                    instance.setDeleted(Boolean.TRUE);
                    instance.setName(Strings.toUpperCase(Constantes.ESTADO_ELIMINADO + Constantes.SEPARADOR + instance.getProduct().getName())); //Registrar el producto que estaba referenciado
                    instance.setDescription(instance.getProduct().toString()); //Registrar el producto que estaba referenciado
                    instance.setProduct(null);
//                    aggregationDetailService.save(instance.getId(), instance); //Actualizar la entidad
//                    aggregation.removeAggregationDetail(instance); //Quitar de la vista
                    this.aggregation.addAggregationDetail(instance); //Reemplaza el detalle con el nuevo valor modificado para borrar
                }
                setOutcome("");
            }
        }
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
