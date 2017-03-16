/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.modules;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author kraftek
 * @date 3/16/2017
 */
public class UpdateBuilder extends AbstractBuilder {
    private Set<String> moduleManifests = new TreeSet<>();

    public UpdateBuilder moduleManifest(String value) {
        moduleManifests.add(value);
        return this;
    }

    @Override
    public String build(boolean standalone) {
        StringBuilder xmlBuilder = new StringBuilder();
        if (standalone) {
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n")
                    .append("<!DOCTYPE module_updates PUBLIC \"-//NetBeans//DTD Autoupdate Catalog 2.5//EN\")")
                    .append("\"http://www.netbeans.org/dtds/autoupdate-catalog-2_5.dtd\">");
        }
        LocalDateTime now = LocalDateTime.now();
        xmlBuilder.append("<module_updates timestamp=\"")
                .append(String.format("%02d",now.getHour())).append("/")
                .append(String.format("%02d",now.getMinute())).append("/")
                .append(String.format("%02d",now.getSecond())).append("/")
                .append(String.format("%02d",now.getDayOfMonth())).append("/")
                .append(String.format("%02d",now.getMonth().getValue())).append("/")
                .append(String.format("%02d",now.getYear())).append("\">\n");
        for (String moduleManifest : moduleManifests) {
            xmlBuilder.append(moduleManifest).append("\n");
        }
        xmlBuilder.append("</module_updates>");
        return xmlBuilder.toString();
    }
}
