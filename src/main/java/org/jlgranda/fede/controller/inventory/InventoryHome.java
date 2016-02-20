/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlgranda.fede.controller.inventory;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.sales.Product;
import org.jpapi.model.Group;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@RequestScoped
public class InventoryHome extends FedeController implements Serializable {
    
    Logger logger = LoggerFactory.getLogger(InventoryHome.class);
    
    @Inject
    private SettingHome settingHome;
    
    private Long productId;
    
    private Product lastProduct;
    
    private Product product;
    
    private List<Product> lastProducts = new ArrayList<>();
    
    @EJB
    private ProductService productService; 

    @PostConstruct
    private void init() {
        setProduct(productService.createInstance());
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public Product getLastProduct() {
        if (lastProduct == null){
            List<Product> obs = productService.findByNamedQuery("Product.findLastProduct", 1);
            lastProduct = obs.isEmpty() ? new Product() : (Product) obs.get(0);
        }
        return lastProduct;
    }

    public void setLastProduct(Product lastProduct) {
        this.lastProduct = lastProduct;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setLastProducts(List<Product> lastProducts) {
        this.lastProducts = lastProducts;
    }

    public List<Product> getLastProducts() {
        int limit = Integer.parseInt(settingHome.getValue("app.fede.sales.dashboard.lasts.list.length", "10"));
        if (lastProducts.isEmpty())
            lastProducts = productService.findByNamedQuery("Product.findLastProducts", limit);
        return lastProducts;
    }

    public boolean mostrarFormularioProducto() {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "550");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "480");
        super.openDialog(settingHome.getValue("app.fede.inventory.popup", "popup_producto"), width, height, true);
        return true;
    }
    
    @Override
    public void handleReturn(SelectEvent event) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void save(){
        productService.save(getProduct().getId(), getProduct());
        this.addDefaultSuccessMessage();
        
        this.closeDialog(getProduct());
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
