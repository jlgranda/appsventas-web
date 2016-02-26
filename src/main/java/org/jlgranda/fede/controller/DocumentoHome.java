/*
 * Copyright (C) 2016 Jorge
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import net.tecnopro.document.ejb.DocumentoService;
import net.tecnopro.document.ejb.TareaService;
import net.tecnopro.document.model.Documento;
import net.tecnopro.document.model.Tarea;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge
 */
@Named(value = "documentoHome")
@RequestScoped
public class DocumentoHome extends FedeController {

    Logger logger = LoggerFactory.getLogger(DocumentoHome.class);

    @Inject
    @LoggedIn
    private Subject subject;
    @Inject
    private SettingHome settingHome;
    @EJB
    private DocumentoService documentoService;
    private Documento documento;
    private Long documentoId;
    @EJB
    private TareaService tareaService;
    private Long tareaId;
    private List<Documento> documentos;
    private List<UploadedFile> uploadedFiles = Collections.synchronizedList(new ArrayList<UploadedFile>());

    @PostConstruct
    public void init() {
        setDocumento(documentoService.createInstance());

    }

    public Documento getDocumento() {
        if (documentoId != null && !this.documento.isPersistent()) {
            this.documento = documentoService.find(documentoId);
        }
        return documento;
    }

    public void setDocumento(Documento documento) {
        this.documento = documento;
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
    }

    public Long getTareaId() {
        return tareaId;
    }

    public void setTareaId(Long tareaId) {
        this.tareaId = tareaId;
    }

    public List<Documento> getDocumentos() {
        if (tareaId != null) {
            Tarea tarea = tareaService.find(tareaId);
            documentos = documentoService.findByNamedQuery("Documento.findByTarea", tarea);
        }
        return documentos;
    }

    public void handleFileUpload(FileUploadEvent event) {
        procesarUploadFile(event.getFile());
    }

    public void procesarUploadFile(UploadedFile file) {

        if (file == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("fede.file.null"));
            return;
        }

        if (subject == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("fede.subject.null"));
            return;
        }
        try {
            if (file.getFileName().endsWith(".pdf")) {
                this.documento.setContents(file.getContents());
            }

        } catch (Exception e) {
            e.printStackTrace();
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), e.getMessage());
        }
    }

    public boolean mostrarFormularioCargaDocumento() {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "550");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "480");
        super.openDialog(SettingNames.POPUP_SUBIR_DOCUMENTO, width, height, true);
        return true;
    }

    public void setDocumentos(List<Documento> documentos) {
        this.documentos = documentos;
    }

    @Override
    public void handleReturn(SelectEvent event) {

    }

    @Override
    public Group getDefaultGroup() {
        return null;
    }

    public List<UploadedFile> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<UploadedFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

}
