/*
 * Copyright (C) 2019 jlgrandadev
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
package org.jlgranda.fede.controller.gift;

import com.jlgranda.fede.ejb.gifts.GiftService;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.controller.admin.TemplateHome;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.gifts.GiftEntity;
import org.jlgranda.fede.ui.model.LazyGiftDataModel;
import org.jpapi.model.Group;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
//import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Ronny Ramirez
 */
@Named
@RequestScoped
public class GiftHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 1L;

    String uuid;

    @Inject
    private Subject subject;

    private LazyGiftDataModel lazyGiftDataModel;

    @EJB
    private GiftService giftService;

    @Inject
    private SettingHome settingHome;
    
    @Inject
    private TemplateHome templateHome;

    @PostConstruct
    private void init() {
        setStart(Dates.minimumDate(Dates.addDays(Dates.now(), -7)));
        setEnd(Dates.maximumDate(Dates.now()));
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Executa el Servlet de generación de imagenes de cupón
     */
    public void print() {

        try {
            if (getCountGiftByOwner(subject) < 3){
                String code = subject.getCode();
                String name = subject.getFirstname() + " " + subject.getSurname();
                String uuId = UUID.randomUUID().toString();
                String baseImage = "emporio-lojano-billete.jpg";

                //Ejecutar servlet
                redirectTo("/giftServlet/?code=" + code + "&name=" + name 
                        + "&uuid=" + uuId + "&baseImage=" + baseImage);

                //Enviar a la base de datos
                GiftEntity giftEntity = giftService.createInstance();
                giftEntity.setUuid(uuId);
                giftEntity.setImageBasePath(settingHome.getValue("app.gift.baseImagePath", "/var/opt/appsventas/img/") + baseImage);
                giftEntity.setOwner(subject);
                giftEntity.setAuthor(subject);
                giftEntity.setDescription(settingHome.getValue("app.gift.defaultDescription", "Cupón de descuento válido para consumos señalados en la imagen dentro del local participante."));
                giftService.save(giftEntity);
                //Notificar via correo
                if (true){ //TODO alguna condición
                    Map<String, Object> values = new HashMap<>();
                    values.put("subject", subject);
                    values.put("summary", giftEntity.getSummary()); //El resumen directo
                    values.put("url", "http://emporiolojano.com:8080/appsventas-web/home.jsf");
                    values.put("url_title", "Dolar directo");
                    sendNotification(templateHome, settingHome, subject, values, "app.mail.template.gift.notify", false);
                }
            } else {
                this.addWarningMessage("¡No te pierdas tus regalos!", "Aún tiene regalos sin efectivizar.");
            }

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(GiftHome.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void handleReturn(SelectEvent event) {

    }

    @Override
    public Group getDefaultGroup() {
        return new Group();
    }

    @Override
    protected void initializeDateInterval() {

    }

    @Override
    public List<Group> getGroups() {
        return new LinkedList<>();
    }

    public void clear() {
        filter();
    }

    public LazyGiftDataModel getLazyGiftDataModel() {

        filter();

        return lazyGiftDataModel;
    }

    public void setLazyGiftDataModel(LazyGiftDataModel lazyGiftDataModel) {
        this.lazyGiftDataModel = lazyGiftDataModel;
    }

    /**
     * Filtro para llenar el Lazy Datamodel
     */
    public void filter() {

        if (lazyGiftDataModel == null) {
            lazyGiftDataModel = new LazyGiftDataModel(giftService);
        }

        lazyGiftDataModel.setOwner(subject);
        lazyGiftDataModel.setStart(getStart());
        lazyGiftDataModel.setEnd(getEnd());

        /* if (getKeyword() != null && getKeyword().startsWith("label:")) {
            String parts[] = getKeyword().split(":");
            if (parts.length > 1) {
                lazyGiftDataModel.setTags(parts[1]);
            }
            lazyGiftDataModel.setFilterValue(null);//No buscar por keyword
        } else {
            lazyGiftDataModel.setTags(getTags());
            lazyGiftDataModel.setFilterValue(getKeyword());
        }*/
    }

    
    public Long getGiftCount() {
        // return giftService.count("gift.countGiftByOwner", subject);
        return getCountGiftByOwner(subject);
    }

    public List<GiftEntity> getLastGitfs(){
        return getLastGiftsFromOwner(this.subject, this.getStart(), this.getEnd());
    }
    
    public Long getCountGiftByOwner(Subject _subject) {
        return giftService.count("gift.countGiftByOwner", _subject, StatusType.ACTIVE.toString());
    }

    public Long getSharedGiftCount() {
        return giftService.count("gift.countGiftSharedByOwner", subject, StatusType.ACTIVE.toString());
    }

    public List<GiftEntity> getGiftsFromOthers() {
        List<GiftEntity> gifts = giftService.findByNamedQuery("gift.giftsFromOtherUsers", subject, getStart(), getEnd(), StatusType.ACTIVE.toString());
        List<GiftEntity> collect = gifts.stream().limit(Long.valueOf(settingHome.getValue("app.gift.others.list.size", "5"))).collect(Collectors.toList()); // truncate to first 10 elements
        return collect;
    }
    
    public List<GiftEntity> getLastGiftsFromOwner(Subject _subject, Date _start, Date _end) {
        return giftService.findByNamedQuery("gift.giftsFromOwner", _subject, _start, _end, StatusType.ACTIVE.toString());
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
