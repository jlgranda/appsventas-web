/*
 * Copyright (C) 2016 jlgranda
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
package net.tecnopro.controller;

import com.google.common.base.Strings;
import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.SubjectService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import net.tecnopro.document.ejb.DocumentoService;
import net.tecnopro.document.ejb.InstanciaProcesoService;
import net.tecnopro.document.ejb.TareaService;
import net.tecnopro.document.model.Documento;
import net.tecnopro.document.model.EstadoTipo;
import net.tecnopro.document.model.InstanciaProceso;
import net.tecnopro.document.model.Tarea;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.ui.util.SubjectConverter;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Lists;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@ManagedBean
@ViewScoped
public class InstanciaProcesoHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(InstanciaProcesoHome.class);

    @Inject
    @LoggedIn
    private Subject subject;

    private Long instanciaProcesoId;

    private InstanciaProceso instanciaProceso;

    @Inject
    private SettingHome settingHome;

    @EJB
    private InstanciaProcesoService instanciaProcesoService;
    @EJB
    private TareaService tareaService;

    private Tarea tarea;

    private List<Tarea> tareas;
    private Documento documento;
    private Subject solicitante;

    private Subject destinatario;

    @EJB
    private SubjectService subjectService;

    @EJB
    private DocumentoService documentoService;

    private List<Documento> documentosRemovidos = new ArrayList<>();

    //UI variables
    private String activeIndex;

    @PostConstruct
    public void init() {
        int amount = 0;
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "360"));
        } catch (java.lang.NumberFormatException nfe) {
            amount = 30;
        }

        setEnd(Dates.now());
        setStart(Dates.addDays(getEnd(), -1 * amount));

        setOutcome("procesos");

        setTarea(tareaService.createInstance()); //Siempre listo para recibir la respuesta del proceso
        setDocumento(documentoService.createInstance());
        //TODO Establecer temporalmente la organización por defecto
        //getOrganizationHome().setOrganization(organizationService.find(1L));
    }

    public Long getInstanciaProcesoId() {
        return instanciaProcesoId;
    }

    public void setInstanciaProcesoId(Long instanciaProcesoId) {
        this.instanciaProcesoId = instanciaProcesoId;
    }

    public InstanciaProceso getInstanciaProceso() {
        if (this.instanciaProcesoId != null && this.instanciaProceso == null) {
            this.instanciaProceso = instanciaProcesoService.find(instanciaProcesoId);
        }
        return instanciaProceso;
    }

    public void setInstanciaProceso(InstanciaProceso instanciaProceso) {
        this.instanciaProceso = instanciaProceso;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Tarea getTarea() {
        return tarea;
    }

    public void setTarea(Tarea tarea) {
        this.tarea = tarea;
    }

    public Subject getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Subject solicitante) {
        this.solicitante = solicitante;
    }

    public Subject getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Subject destinatario) {
        this.destinatario = destinatario;
    }

    public void send() {
        try {

            //Actualizar tarea pendiente
            for (Tarea t : getInstanciaProceso().getTareas()) {
                if (EstadoTipo.ESPERA.equals(t.getEstadoTipo())) {
                    t.setEstadoTipo(EstadoTipo.RESUELTO);
                }
            }

            //Preparar tarea para envio
            tarea.setAuthor(subject);
            tarea.setOwner(getDestinatario());
            tarea.setDepartamento("Temporal");
            tarea.setEstadoTipo(EstadoTipo.ESPERA);
            getInstanciaProceso().addTarea(tarea);
            procesarDocumentos(tarea);
            eliminarDocumentos();
            instanciaProcesoService.save(getInstanciaProceso().getId(), getInstanciaProceso());
            //Encerar tarea para recoger nueva respuesta
            setTarea(tareaService.createInstance());
            setDestinatario(null);
            setActiveIndex(null);

            this.addDefaultSuccessMessage();
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
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
            Documento doc = crearDocumento(file, tarea);
            if (tarea != null) {
                tarea.getDocumentos().add(doc);
            }
            //Encerar el obeto para edición de nuevo documento
            setDocumento(documentoService.createInstance());

        } catch (Exception e) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), e.getMessage());
        }
    }

    public void procesarDocumentos(Tarea t) {
        for (Documento doc : t.getDocumentos()) {
            generaDocumento(new File(doc.getRuta()), doc.getContents());
        }
    }

    public StreamedContent downloadDocument(Documento doc) {
        StreamedContent fileDownload = null;
        try {
            if (doc != null) {
                InputStream stream = new FileInputStream(new File(doc.getRuta()));

                fileDownload = new DefaultStreamedContent(stream, doc.getMimeType(), doc.getFileName());
            }
        } catch (FileNotFoundException e) {
        }
        return fileDownload;
    }

    public void eliminarDocumentos() {
        for (Documento doc : documentosRemovidos) {
            if (doc.isPersistent()) {
                documentoService.remove(doc.getId(), doc);
            }
            documentosRemovidos.remove(doc);
        }
    }

    public void removerDocumento(Documento doc, Tarea t) {
        this.documentosRemovidos.add(doc);
        t.getDocumentos().remove(doc);
    }

    public void generaDocumento(File file, byte[] bytes) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try (BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                bos.write(bytes);
                bos.flush();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            addErrorMessage(ex, I18nUtil.getMessages("common.error.uploadfail"));
        }
    }

    public List<Subject> completeSubjects(final String query) {
        List<Subject> result = new ArrayList<>();
        if (!"".equals(query.trim())) {
            Subject subjectBuscar = new Subject();
            subjectBuscar.setUsername(query);
            subjectBuscar.setFirstname(query);
            subjectBuscar.setSurname(query);
            result = subjectService.buscarPorCriterio(subjectBuscar);
        }
        SubjectConverter.setSubjects(result);
        return result;
    }

    public void setActiveIndex(String activeIndex) {
        this.activeIndex = activeIndex;
    }

    /**
     * Calcula los indices de los tabs a mostrar expandidos, en función del
     * estado de las tareas de la instancia de proceso
     *
     * @return lista de indices separadas por coma
     */
    public String getActiveIndex() {
        if (Strings.isNullOrEmpty(activeIndex)) {
            if (getInstanciaProceso() == null) {
                activeIndex = "";
            } else {
                List<Integer> indexs = new ArrayList<>();
                for (int i = 0; i < getInstanciaProceso().getTareas().size(); i++) {
                    if (EstadoTipo.ESPERA.equals(getInstanciaProceso().getTareas().get(i).getEstadoTipo())) {
                        indexs.add(i);
                    }
                }
                activeIndex = Lists.toString(indexs).trim();
            }
        }

        return activeIndex;
    }

    public boolean calculeShowResponseForm() {
        boolean showResponseForm = false;
        if (getInstanciaProceso() != null) {
            for (Tarea t : getInstanciaProceso().getTareas()) {
                //Sólo hay una tarea a la espera y corresponde al usuario actual
                if (EstadoTipo.ESPERA.equals(t.getEstadoTipo()) && subject.equals(t.getOwner())) {
                    showResponseForm = true;
                    break; //finalizar bucle, ya se econtro.
                }
            }
        }
        return showResponseForm;
    }

    private Documento crearDocumento(UploadedFile file, Tarea t) {
        Documento doc = documentoService.createInstance();
        doc.setTarea(t);
        doc.setOwner(t.getOwner());
        doc.setAuthor(t.getOwner());

        if (getDocumento() != null && Strings.isNullOrEmpty(getDocumento().getName())) {
            doc.setName(file.getFileName());
            doc.setDocumentType(DocumentType.UNDEFINED);
        } else {
            doc.setName(getDocumento().getName());
            doc.setDocumentType(getDocumento().getDocumentType());
        }
        doc.setFileName(file.getFileName());
        doc.setNumeroRegistro(UUID.randomUUID().toString());

        doc.setRuta(settingHome.getValue("app.management.tarea.documentos.ruta", "/tmp") + "//" + file.getFileName());

        doc.setMimeType(file.getContentType());

        /**
         * Permite que el documento tenga asignado los bytes para posteriormete
         * con dichos bytes generar el documento digital y guardarlo en la ruta
         * definida
         */
        doc.setContents(file.getContents());
        return doc;
    }

    public Documento getDocumento() {
        return documento;
    }

    public void setDocumento(Documento documento) {
        this.documento = documento;
    }

    public List<Documento> getDocumentosRemovidos() {
        return documentosRemovidos;
    }

    public void setDocumentosRemovidos(List<Documento> documentosRemovidos) {
        this.documentosRemovidos = documentosRemovidos;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}