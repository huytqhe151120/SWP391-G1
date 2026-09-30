package com.swp391.g1.dao;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Staff;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StaffDAO extends DBContext {

    public List<Staff> getAll() throws SQLException {
        String sql = """
                SELECT s.id, s.code, s.name, s.dob, s.gender,
                       s.status, s.account_id,
                       a.departmentInfo
                FROM staff s
                OUTER APPLY (
                    SELECT STRING_AGG(
                               CAST(
                                   CONCAT(
                                       d.code,
                                       N' - ',
                                       d.name,
                                       CASE
                                           WHEN sa.position IS NOT NULL
                                               THEN CONCAT(N' (', sa.position, N')')
                                           ELSE N''
                                       END
                                   ) AS nvarchar(max)
                               ),
                               N'; '
                           ) AS departmentInfo
                    FROM staff_assignment sa
                    INNER JOIN department d ON d.id = sa.department_id
                    WHERE sa.staff_id = s.id
                ) a
                ORDER BY s.code
                """;

        List<Staff> staffList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                staffList.add(mapRow(resultSet));
            }
        }

        return staffList;
    }

    private Staff mapRow(ResultSet resultSet) throws SQLException {
        Staff staff = new Staff();

        staff.setId(resultSet.getInt("id"));
        staff.setCode(resultSet.getString("code"));
        staff.setName(resultSet.getString("name"));

        Date dob = resultSet.getDate("dob");
        staff.setDob(dob == null ? null : dob.toLocalDate());

        boolean gender = resultSet.getBoolean("gender");
        staff.setGender(resultSet.wasNull() ? null : gender);

        staff.setStatus(resultSet.getString("status"));

        int accountId = resultSet.getInt("account_id");
        staff.setAccountId(resultSet.wasNull() ? null : accountId);

        staff.setDepartmentInfo(resultSet.getString("departmentInfo"));

        return staff;
    }
}
