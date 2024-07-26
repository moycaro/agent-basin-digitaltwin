package org.carol.tfm.external_planners.domain.entities;

import java.util.List;

public class ExternalPlannerResponse {
    List<DamnManagement> damnManagementList;

    public List<DamnManagement> getDamnManagementList() {
        return damnManagementList;
    }

    public void setDamnManagementList(List<DamnManagement> damnManagementList) {
        this.damnManagementList = damnManagementList;
    }
}
