package com.swp391.g1.dao;

import com.swp391.g1.model.Staff;

import java.util.List;

public interface IStaffDAO extends IGenericDAO<Staff, Integer> {
    List<Staff> searchStaff(String keyword, String id, String code, String name, String dob,
                            String gender, String accountId, String status);

    Staff findByAccountId(int accountId);

    Staff findFirst();

    List<Staff> findByDepartmentId(int departmentId);
}
