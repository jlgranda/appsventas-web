/*
 * Copyright (C) 2018 jlgranda
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
package org.jlgranda.fede.controller.sales;

import com.jlgranda.fede.ejb.FacturaElectronicaService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import org.jlgranda.fede.controller.FacturaElectronicaHome;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.Payment;
import org.jpapi.model.Group;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@ViewScoped
@Named
public class SummaryHome  extends FedeController implements Serializable {

    private static final long serialVersionUID = 145155795064685887L;
    
    Logger logger = LoggerFactory.getLogger(SummaryHome.class);

    @Inject
    private Subject subject;

    private Subject customer;
    
    @Inject
    private SettingHome settingHome;
    
    @EJB
    private InvoiceService invoiceService;

    @Inject 
    private FacturaElectronicaHome facturaElectronicaHome;
    
    @EJB
    private FacturaElectronicaService facturaElectronicaService;
    
    //Calcular Resumen
    private BigDecimal grossSalesTotal;
    private BigDecimal discountTotal;
    private BigDecimal salesTotal;
    private BigDecimal purchaseTotal;
    private BigDecimal costTotal;
    private BigDecimal profilTotal;

    /**
     * Selector de grupos de fechas
     */
    private Date grupoFechas;
    
    @PostConstruct
    private void init() {

        int range = 0; //Rango de fechas para visualiar lista de entidades
        try {
            //range = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD__SUMMARY_RANGE, "0"));
            range = Integer.valueOf(settingHome.getValue("fede.dashboard.summary.range", "0"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 1;
        }
        
        setGrossSalesTotal(BigDecimal.ZERO);
        setDiscountTotal(BigDecimal.ZERO);
        setSalesTotal(BigDecimal.ZERO);
        setPurchaseTotal(BigDecimal.ZERO);
        setCostTotal(BigDecimal.ZERO);
        setProfilTotal(BigDecimal.ZERO);
        
        
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
        
        setOutcome("dashboard");

        calculeSummary();
        
    }

    public BigDecimal getGrossSalesTotal() {
        return grossSalesTotal;
    }

    public void setGrossSalesTotal(BigDecimal grossSalesTotal) {
        this.grossSalesTotal = grossSalesTotal;
    }
    
    public BigDecimal getSalesTotal() {
        return salesTotal;
    }

    public void setSalesTotal(BigDecimal salesTotal) {
        this.salesTotal = salesTotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(BigDecimal discountTotal) {
        this.discountTotal = discountTotal;
    }

    public BigDecimal getPurchaseTotal() {
        return purchaseTotal;
    }

    public void setPurchaseTotal(BigDecimal purchaseTotal) {
        this.purchaseTotal = purchaseTotal;
    }

    public BigDecimal getCostTotal() {
        return costTotal;
    }

    public void setCostTotal(BigDecimal costTotal) {
        this.costTotal = costTotal;
    }

    public BigDecimal getProfilTotal() {
        return profilTotal;
    }

    public void setProfilTotal(BigDecimal profilTotal) {
        this.profilTotal = profilTotal;
    }

    public Date getGrupoFechas() {
        return grupoFechas;
    }

    public void setGrupoFechas(Date grupoFechas) {
        this.grupoFechas = grupoFechas;
    }
    
    public void  calculeSummary() {
        Date _start = Dates.minimumDate(getStart());
        Date _end = Dates.maximumDate(getEnd());
        calculeSummary(_start, _end);
        
    }
    public void  calculeSummary(Date _start, Date _end) {
        if (Dates.calculateNumberOfDaysBetween(_start, _end) <= 0){
            int range = Integer.parseInt(settingHome.getValue("fede.dashboard.summary.range", "1"));
            _start = Dates.addDays(getStart(), -1 * range);
        }
        this.costTotal = BigDecimal.ZERO;
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesDiscountBetween", 0, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects.stream().forEach((Object[] object) -> {
            this.grossSalesTotal = (BigDecimal) object[0];
            this.discountTotal = (BigDecimal) object[1];
            this.salesTotal = (BigDecimal) object[2];
        });        
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalBetween", 0, this.subject, _start, _end);
        objects.stream().forEach((Object object) -> {
            this.purchaseTotal = (BigDecimal) object;
        });  
        
        if (this.salesTotal == null) {
            this.salesTotal = BigDecimal.ZERO;
        } 
        
        if (this.purchaseTotal == null) {
            this.purchaseTotal = BigDecimal.ZERO;
        }
        
        this.profilTotal = this.salesTotal.subtract(this.purchaseTotal.add(this.costTotal));
        
    }
    
    public List<Invoice> findInvoices(Subject author, DocumentType documentType, int limit, Date start, Date end){   
        if (author == null){ //retornar todas
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", limit, documentType, true, start, end);
        } else {
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentTypeAndAuthor", limit, documentType, author, true, start, end);
        }
    }
    
    public BigDecimal calculeTotal(List<Invoice> list) {
        BigDecimal subtotal = new BigDecimal(0);
        BigDecimal discount = new BigDecimal(0);
        Payment payment = null;
        for (Invoice i : list) {
            subtotal = subtotal.add(i.getTotal());
            payment = i.getPayments().isEmpty() ? null : i.getPayments().get(0);
            if (payment != null )
                discount = discount.add(payment.getDiscount());
        }

        return subtotal.subtract(discount, MathContext.UNLIMITED);
    }
    
    /**
     * Calcular el equivalente en porcentaje para la UI
     * @return 
     */    
    public BigDecimal calculeProfitRate(){
        BigDecimal goal = BigDecimal.valueOf(Long.valueOf(settingHome.getValue("app.fede.sales.goal", "500")));
        this.refresh();
        BigDecimal profitRate = this.getProfilTotal().divide(goal);
        return  profitRate; 
    }
    
    public int calculeProfitRateEquivalentForUX(){
        BigDecimal bd = calculeProfitRate().multiply(BigDecimal.valueOf(100));
        bd.setScale(1, BigDecimal.ROUND_CEILING);
        return bd.intValue();
    }
    
    public BigDecimal countSalesToday() {
        Date _start = Dates.minimumDate(getStart());
        Date _end = Dates.maximumDate(getEnd());
        if (Dates.calculateNumberOfDaysBetween(_start, _end) <= 0){
            int range = Integer.parseInt(settingHome.getValue("fede.dashboard.summary.range", "1"));
            _start = Dates.addDays(getStart(), -1 * range);
        }
        BigDecimal total = new BigDecimal(invoiceService.count("Invoice.countTotalInvoiceBetween", this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end));
        return total;
    }
    
    public BigDecimal calculeAverage(){
        clear();
        Date yesterday = Dates.addDays(Dates.now(), -1);
        calculeSummary(Dates.addDays(yesterday, -10), yesterday);
        return getProfilTotal().divide(BigDecimal.TEN);
    }
    
    public BigDecimal calculeProfitRateAverage(){
        BigDecimal goal = BigDecimal.valueOf(Long.valueOf(settingHome.getValue("app.fede.sales.goal", "500")));
        return  calculeAverage().divide(goal);
    }
    
    

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
   
    public void clear() {
        setGrossSalesTotal(BigDecimal.ZERO);
        setDiscountTotal(BigDecimal.ZERO);
        setSalesTotal(BigDecimal.ZERO);
        setPurchaseTotal(BigDecimal.ZERO);
        setCostTotal(BigDecimal.ZERO);
        setProfilTotal(BigDecimal.ZERO);
    }
    
    /**
     * Refrescar la vista y calculos
     */
    public void refresh() {
        clear();
        calculeSummary();
    }
    
    
    /**
     * Refrescar para un grupo de fechas
     */
    public void refreshPorGrupoFechas(){
        clear();
        setStart(grupoFechas);
        setEnd(grupoFechas);
        calculeSummary();
    }

}
