/*
 * Copyright (C) 2021 jlgranda
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
package org.jlgranda.fede.controller;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.AccountService;
import com.jlgranda.fede.ejb.GroupService;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.ui.model.LazyAccountDataModel;
import org.jpapi.util.I18nUtil;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class AccountHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -1007161141552849702L;

    Logger logger = LoggerFactory.getLogger(AccountHome.class);

    @Inject
    private Subject subject;

    @Inject
    private SettingHome settingHome;

    @EJB
    private GroupService groupService;

    private LazyAccountDataModel lazyDataModel;

    private Account account;

    private Long accountId;

    @EJB
    private AccountService accountService;

    @PostConstruct
    private void init() {
        int range = 0;
        try {
            range = Integer.valueOf(settingHome.getValue(SettingNames.ACCOUNT_TOP_RANGE, "7"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 7;
        }
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
        setAccount(accountService.createInstance());//Instancia de Cuenta
        setOutcome("accounts");
        filter();
    }

    @Override
    public void handleReturn(SelectEvent event) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    @Override
    public List<Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "inventory", "inventory"), Group.Type.LABEL));
        }
        return this.groups;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Account getAccount() {
        if (this.accountId != null && !this.account.isPersistent()) {
            this.account = accountService.find(accountId);
        }
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public LazyAccountDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyAccountDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public void clear() {
        filter();
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyAccountDataModel(accountService);
        }
        lazyDataModel.setOwner(this.subject);
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

    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                Account p = (Account) event.getObject();
                redirectTo("/pages/fede/accounting/account.jsf?accountId=" + p.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
    }

    public void save() {
        if (account.isPersistent()) {
            account.setLastUpdate(Dates.now());
        } else {
            account.setAuthor(this.subject);
            account.setOwner(this.subject);
        }
        accountService.save(account.getId(), account);
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
