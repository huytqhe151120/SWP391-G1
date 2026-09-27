package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.PartnerCompany;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PartnerCompanyDAOImpl implements IPartnerCompanyDAO {

    @Override
    public PartnerCompany findByCode(String code) {
        return null;
    }

    @Override
    public List<PartnerCompany> findByCommonStatus(Enum.CommonStatus status) {
        List<PartnerCompany> list = new ArrayList<>();
        String sql = "SELECT * FROM partner_company"; // Lấy theo tên bảng

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PartnerCompany pc = new PartnerCompany();
                pc.setId(rs.getInt("id"));
                pc.setCode(rs.getString("code"));
                pc.setName(rs.getString("name"));
                // ... map các trường khác
                pc.setStatus(Enum.CommonStatus.valueOf(rs.getString("status"))); // Map Enum

                list.add(pc);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<T> findAll() {
        return List.of();
    }

    @Override
    public T findById(K id) {
        return null;
    }

    @Override
    public K insert(T entity) {
        return null;
    }

    @Override
    public boolean update(T entity) {
        return false;
    }

    @Override
    public boolean delete(K id) {
        return false;
    }
}
