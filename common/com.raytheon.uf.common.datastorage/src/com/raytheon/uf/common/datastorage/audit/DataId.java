/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastorage.audit;

import java.util.HashSet;
import java.util.Set;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Identifies where the data in a data storage operation is going in the data
 * store.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class DataId {

    @DynamicSerializeElement
    private String traceId;

    @DynamicSerializeElement
    private String file;

    @DynamicSerializeElement
    private String group;

    @DynamicSerializeElement
    private Set<String> datasets;

    public DataId() {
    }

    public DataId(String traceId, String file, String group) {
        this(traceId, file, group, new HashSet<>());
    }

    public DataId(String traceId, String file, String group,
            Set<String> datasets) {
        this.traceId = traceId;
        this.file = file;
        this.group = group;
        this.datasets = datasets;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Set<String> getDatasets() {
        return datasets;
    }

    public void setDatasets(Set<String> datasets) {
        this.datasets = datasets;
    }

    public void addDataset(String dataset) {
        datasets.add(dataset);
    }

    public String[] getFullyQualifiedDatasets() {
        return datasets.stream().map(dataset -> group + '/' + dataset)
                .toArray(String[]::new);
    }

    @Override
    public String toString() {
        return "DataId [traceId=" + traceId + ", file=" + file + ", group="
                + group + ", datasets=" + datasets + "]";
    }
}