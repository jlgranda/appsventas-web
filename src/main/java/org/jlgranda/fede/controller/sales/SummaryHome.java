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
import com.jlgranda.fede.ejb.sales.ProductCache;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.jlgranda.fede.model.sales.Product;
import org.jpapi.model.Group;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartSeries;
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
    private FacturaElectronicaHome facturaElectronicaHome;
    
    @EJB
    private FacturaElectronicaService facturaElectronicaService;
    
    @Inject
    private SettingHome settingHome;
            
    @EJB
    private ProductService productService; 
    
    @EJB
    private ProductCache productCache;
    
    @EJB
    private InvoiceService invoiceService;    
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

        initializeDateInterval();
        
        setGrossSalesTotal(BigDecimal.ZERO);
        setDiscountTotal(BigDecimal.ZERO);
        setSalesTotal(BigDecimal.ZERO);
        setPurchaseTotal(BigDecimal.ZERO);
        setCostTotal(BigDecimal.ZERO);
        setProfilTotal(BigDecimal.ZERO);
        
        setOutcome("dashboard");
        calculeSummary();
        createBarModelAmount();
        createbarModelSales();
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
        
        this.costTotal = BigDecimal.ZERO;
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesDiscountBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects.stream().forEach((Object[] object) -> {
            this.grossSalesTotal = (BigDecimal) object[0];
            this.discountTotal = (BigDecimal) object[1];
            this.salesTotal = (BigDecimal) object[2];
        });        
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalBetween", Integer.MAX_VALUE, this.subject, _start, _end);
        objects.stream().forEach((Object object) -> {
            this.purchaseTotal = (BigDecimal) object;
        });  
        
        if (this.discountTotal == null) {
            this.discountTotal = BigDecimal.ZERO;
        }        
        
        if (this.salesTotal == null) {
            this.salesTotal = BigDecimal.ZERO;
        } 
        
        if (this.purchaseTotal == null) {
            this.purchaseTotal = BigDecimal.ZERO;
        }
        
        this.salesTotal = this.salesTotal.subtract(this.discountTotal);
        this.profilTotal = this.salesTotal.subtract(this.purchaseTotal.add(this.costTotal));
        
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
    
    public BigDecimal calculeAverage(int days){
        clear();
        Date yesterday = Dates.addDays(Dates.now(), -1);
        calculeSummary(Dates.minimumDate(Dates.addDays(yesterday, -1 * (days - 1))), Dates.maximumDate(yesterday));
        return getProfilTotal().divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal calculeProfitRateAverage(int days){
        BigDecimal goal = BigDecimal.valueOf(Long.valueOf(settingHome.getValue("app.fede.sales.goal", "500")));
        return  calculeAverage(days).divide(goal);
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
    public void clearBarModel(){
        setBarModelAmount(null);
        setBarModelSales(null);
    }
    private BarChartModel barModelAmount;
    private BarChartModel barModelSales; 
    public BarChartModel getBarModelAmount(){
        if(barModelAmount==null){
            setBarModelAmount(createBarModelAmount());
        }
        return barModelAmount;
    }
    public void setBarModelAmount(BarChartModel barModelAmount){
        this.barModelAmount = barModelAmount;
    }       
    public BarChartModel getBarModelSales(){
        if(barModelSales==null){
            setBarModelSales(createbarModelSales());
        }
        return barModelSales;
    }
    public void setBarModelSales(BarChartModel barModelSales){
        this.barModelSales = barModelSales;
    }
    private BarChartModel createBarModelAmount(){
        BarChartModel model = new BarChartModel();        
        ChartSeries product = new ChartSeries();        
        product.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.label"));        
        int top = Integer.valueOf(settingHome.getValue("app.fede.inventory.top", "10"));
        List<Object[]> objects = productService.findObjectsByNamedQueryWithLimit("Product.findTopProductIdsBetween", top, getStart(), getEnd());
        objects.stream().forEach((Object[] object) -> {
            Product _product = productCache.lookup((Long) object[0]);
            if (_product != null){
                _product.getStatistics().setCount((Double) object[1]);
                product.set(_product.getName(),_product.getStatistics().getCount());
            }
        });        
        model.addSeries(product);        
        model.setTitle(I18nUtil.getMessages("app.fede.barchart.sales.amount.title"));
        model.setLegendPosition(settingHome.getValue("app.fede.barchart.sales.legendPosition", "e"));  
        model.setStacked(true);
        model.setAnimate(false);
        model.setShowPointLabels(false);        
        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.barchart.sales.axis"));
        model.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = model.getAxis(AxisType.Y);        
        yAxis.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.amount.axis"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.barchart.sales.amount.min", "0")));
        yAxis.setMax(Integer.valueOf(settingHome.getValue("app.fede.barchart.sales.amount.max", "150")));
        
        return model;
    }

    private BarChartModel createbarModelSales(){        
        BarChartModel model = new BarChartModel();        
        ChartSeries product = new ChartSeries();        
        product.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.label"));              
        int top = Integer.valueOf(settingHome.getValue("app.fede.inventory.top", "10"));
        List<Object[]> objects = productService.findObjectsByNamedQueryWithLimit("Product.findTopProductIdsBetweenPrice", top, getStart(), getEnd());
        objects.stream().forEach((Object[] object) -> {
            Product _product = productCache.lookup((Long) object[0]);
            if (_product != null){
                _product.getStatistics().setCount((Double) object[1]);
                product.set(_product.getName(), _product.getStatistics().getCount());                
            }
        });        
        model.addSeries(product);        
        model.setTitle(I18nUtil.getMessages("app.fede.barchart.sales.price.title"));
        model.setLegendPosition(settingHome.getValue("app.fede.barchart.sales.legendPosition", "e"));  
        model.setStacked(true);
        model.setAnimate(false);
        model.setShowPointLabels(false);        
        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.barchart.sales.axis"));
        model.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = model.getAxis(AxisType.Y);        
        yAxis.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.price.axis"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.barchart.sales.price.min", "0")));
        yAxis.setMax(Integer.valueOf(settingHome.getValue("app.fede.barchart.sales.price.max", "200")));
        
        return model;
    }

    @Override
    protected void initializeDateInterval() {
        int range = 0; //Rango de fechas para visualiar lista de entidades
        try {
            range = Integer.valueOf(settingHome.getValue("fede.dashboard.summary.range", "0"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 1;
        }        
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
    }
}