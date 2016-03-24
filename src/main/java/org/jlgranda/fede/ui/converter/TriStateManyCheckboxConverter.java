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
package org.jlgranda.fede.ui.converter;

/**
 *
 * @author jlgranda
 * @basedon https://github.com/primefaces-extensions/showcase/blob/master/src/main/java/org/primefaces/extensions/showcase/converter/TriStateManyCheckboxConverter.java
 */
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;


/**
 * TriStateManyCheckboxConverter converter class.
 *
 * @author  Mauricio Fenoglio / last modified by $Author:$
 * @version $Revision:$
 */
@FacesConverter("triStateManyCheckboxConverter")
public class TriStateManyCheckboxConverter implements Converter {

	public Object getAsObject(final FacesContext context, final UIComponent component, final String value) {
		State res;
		if (value.equals("0")) {
			res = new State("One");
		} else if (value.equals("1")) {
			res = new State("Two");
		} else {
			res = new State("Three");
		}

		return res;
	}

	public String getAsString(final FacesContext context, final UIComponent component, final Object valueO) {
		State value = (State) valueO;
		String res;
		if (value.getState().equals("One")) {
			res = "0";
		} else if (value.getState().equals("Two")) {
			res = "1";
		} else {
			res = "2";
		}

		return res;
	}
}
