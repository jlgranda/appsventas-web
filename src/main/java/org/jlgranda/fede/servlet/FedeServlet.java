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
package org.jlgranda.fede.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jpapi.util.Strings;

/**
 *
 * @author Jorge
 */
//@WebServlet(name = "fedeServlet", urlPatterns = {"/fedeServlet/*"})
public class FedeServlet extends HttpServlet {

//    @Inject
//    private DocumentoService documentoService;
//    @Inject
//    private SubjectService subjectService;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            
            String entity = request.getParameter("entity");
            String entityId = request.getParameter("id");
            System.err.println("entity: " + entity + ", id=" + entityId);
            File file;
            byte[] contents;
            if (entity == null) {
                return;
            }
            switch (entity) {
                case "invoice":
                    if (Strings.isNullOrEmpty(entityId)) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
                        System.err.println("entity: " + entity + ", id=" + entityId + ", ERROR: // 404.");
                        return;
                    }
                    file = new File("/tmp/" + entityId + ".pdf");
                    contents = getContent(file);
                    if (contents == null) {
                        response.sendError(HttpServletResponse.SC_NO_CONTENT); // 404.
                        System.err.println("entity: " + entity + ", id=" + entityId + ", ERROR: // " + HttpServletResponse.SC_NO_CONTENT);
                        return;
                    }
                    response.setCharacterEncoding("ISO-8859-1");
                    response.setContentType("application/pdf");
                    response.setContentLength(contents.length);
                    response.getOutputStream().write(contents);
                    response.getOutputStream().flush();
                    response.getOutputStream().close();
                    break;
                case "documento":
//                    Documento documento = documentoService.find(Long.parseLong(entityId));
//                    if (documento == null) {
//                        response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
//                        return;
//                    }
//                    file=  new File(documento.getRuta());
//                    contents = getContent(file);
//                    documento.setContents(contents != null ? contents : null);
//                    if (documento.getContents() == null) {
//                        response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
//                        return;
//                    }
//                    response.setCharacterEncoding("ISO-8859-1");
//                    response.setContentType("application/pdf");
//                    response.setContentLength(documento.getContents().length);
//                    response.getOutputStream().write(documento.getContents());
//                    response.getOutputStream().flush();
//                    response.getOutputStream().close();
                    break;
                case "subject":
////                    if (entityId == null) {
////                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
////                        return;
////                    }
////                    if (entityId.equalsIgnoreCase("")) {
////                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
////                        return;
////                    }
////                    Subject subject = subjectService.find(Long.parseLong(entityId));
////                    if (subject == null) {
////                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
////                        return;
////                    }
//                    byte[] photo = (byte[]) request.getSession().getAttribute("photoUser");
//
//                    if (photo == null) {
//                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
//                        return;
//                    }
//                    response.reset();
//                    response.setContentType("/image/png");
//                    response.getOutputStream().write(photo);
//                    response.getOutputStream().close();
                    break;
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public byte[] getContent(File file) {
        ByteArrayOutputStream ous = null;
        @SuppressWarnings("UnusedAssignment")
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            @SuppressWarnings("UnusedAssignment")
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        return ous.toByteArray();
    }
}
