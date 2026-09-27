package com.swp391.g1.dao;
import java.util.List;

public interface IGenericDAO {
    List<T> findAll(); // Get all records
    T findById(K id); // Get record with id
    K insert(T entity); // Create new record, return id
    boolean update(T entity); // Update record information
    boolean delete(K id); // Delete record with id
}
