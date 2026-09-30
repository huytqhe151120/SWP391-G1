package com.swp391.g1.dao;

import com.swp391.g1.model.Staff;

public interface IStaffDAO extends IGenericDAO<Staff, Integer> {
    Staff findByAccountId(int accountId);

    Staff findFirst();
}
