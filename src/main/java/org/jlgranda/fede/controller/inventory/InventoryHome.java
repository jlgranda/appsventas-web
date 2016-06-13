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
package org.jlgranda.fede.controller.inventory;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.sales.Product;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
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

    private static final long serialVersionUID = -21640696368253046L;
    
    Logger logger = LoggerFactory.getLogger(InventoryHome.class);
    
    @Inject
    private SettingHome settingHome;

    @EJB
    private GroupService groupService;

    private Long productId;
    
    private Product lastProduct;
    
    private Product product;
    
    private List<Product> lastProducts = new ArrayList<>();
    
    @EJB
    private ProductService productService; 
    
    @Inject
    @LoggedIn
    private Subject subject;

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

    /**
     * Retorna los grupos para este controlador
     * @return
     */
    @Override
    public List<Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "inventory", "inventory"), Group.Type.LABEL));
        }

        return this.groups;
    }
    
    /**
     * Busca objetos <tt>Product</tt> para la clave de búsqueda en las columnas
     * name y code
     * @param keyword
     * @return una lista de objetos <tt>Product</tt> que coinciden con la palabra clave dada.
     */
    public List<Product> find(String keyword) {
        keyword = keyword.trim();
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> columns = new HashMap<>();
        columns.put("code", keyword);
        columns.put("name", keyword);
        filters.put("dummy", columns);
        QueryData<Product> queryData = productService.find(-1, -1, "code, name", QuerySortOrder.ASC, filters);
        return queryData.getResult();
    }
}
