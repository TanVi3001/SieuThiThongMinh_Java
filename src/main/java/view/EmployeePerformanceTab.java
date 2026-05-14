package view;

import java.util.Date;

public class EmployeePerformanceTab extends EmployeePerformancePanel {

    public EmployeePerformanceTab() {
        super();
    }

    public void refreshData(Date from, Date to) {
        loadData();
    }
}
