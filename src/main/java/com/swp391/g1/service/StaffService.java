package com.swp391.g1.service;

import com.swp391.g1.dao.StaffDAO;
import com.swp391.g1.model.Staff;

import java.sql.SQLException;
import java.util.List;

public class StaffService {

    private final StaffDAO staffDAO = new StaffDAO();

    public List<Staff> getAll() throws SQLException {
        return staffDAO.getAll();
    }
}
