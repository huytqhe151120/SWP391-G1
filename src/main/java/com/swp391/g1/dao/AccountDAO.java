package com.swp391.g1.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.model.Account;
import com.swp391.g1.model.AccountProfile;
import com.swp391.g1.model.AccountTypeRole;

/**
 * JDBC data access for the account domain tables.
 *
 * <p>Every statement that contains user input is built with
 * PreparedStatement placeholders. Each DAO method opens its own connection
 * (via the project DBContext convention) and closes it in a finally block.
 */
public class AccountDAO {

    private static final Logger LOGGER = Logger.getLogger(AccountDAO.class.getName());

    /**
     * Column projection used for lists and details. The password column is
     * intentionally NOT included here; see findPasswordById.
     */
    private static final String ACCOUNT_COLUMNS = "id, username, type, role, status";

    /**
     * Search accounts with optional filters. Blank filters are ignored and
     * every dynamic condition uses a parameter placeholder.
     */
    public List<Account> findByCriteria(String search, String type, String role, String status) {
        StringBuilder sql = new StringBuilder("SELECT " + ACCOUNT_COLUMNS + " FROM account WHERE 1 = 1");
        List<String> params = new ArrayList<>();
        if (isNotBlank(search)) {
            sql.append(" AND username LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (isNotBlank(type)) {
            sql.append(" AND type = ?");
            params.add(type.trim());
        }
        if (isNotBlank(role)) {
            sql.append(" AND role = ?");
            params.add(role.trim());
        }
        if (isNotBlank(status)) {
            sql.append(" AND status = ?");
            params.add(status.trim());
        }
        sql.append(" ORDER BY id DESC");

        List<Account> accounts = new ArrayList<>();
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                bindStrings(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        accounts.add(mapAccount(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw translate("list accounts", e);
        }
        return accounts;
    }

    /** Loads a single account without the password column. Returns null if absent. */
    public Account findById(int id) {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM account WHERE id = ?";
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapAccount(rs);
                    }
                }
            }
        } catch (SQLException e) {
            throw translate("find account by id", e);
        }
        return null;
    }

    /**
     * Loads only the password column. Used by the service when an update must
     * preserve an unchanged password, keeping the password out of every
     * normal list/detail query.
     */
    public String findPasswordById(int id) {
        String sql = "SELECT password FROM account WHERE id = ?";
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("password");
                    }
                }
            }
        } catch (SQLException e) {
            throw translate("find account password", e);
        }
        return null;
    }

    /** True when a row with the exact username already exists. */
    public boolean usernameExists(String username) {
        return exists("SELECT 1 FROM account WHERE username = ?", username);
    }

    /** True when a row with the exact username exists and is not the given account id. */
    public boolean usernameExistsExcluding(String username, int excludeId) {
        String sql = "SELECT 1 FROM account WHERE username = ? AND id <> ?";
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, excludeId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw translate("check duplicate username", e);
        }
    }

    /** Inserts an account and returns its generated id. */
    public int create(Account account) {
        String sql = "INSERT INTO account (username, password, type, role, status) VALUES (?, ?, ?, ?, ?)";
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, account.getUsername());
                ps.setString(2, account.getPassword());
                ps.setString(3, account.getType());
                ps.setString(4, account.getRole());
                ps.setString(5, account.getStatus());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
                throw new DataAccessException("No generated key returned after account insert.");
            }
        } catch (SQLException e) {
            throw translate("create account", e);
        }
    }

    /**
     * Updates an account. When the password is empty/null it is left
     * unchanged; otherwise it is persisted too.
     */
    public boolean update(Account account) {
        boolean withPassword = account.getPassword() != null && !account.getPassword().isEmpty();
        String sql;
        if (withPassword) {
            sql = "UPDATE account SET username = ?, password = ?, type = ?, role = ?, status = ? WHERE id = ?";
        } else {
            sql = "UPDATE account SET username = ?, type = ?, role = ?, status = ? WHERE id = ?";
        }
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setString(index++, account.getUsername());
                if (withPassword) {
                    ps.setString(index++, account.getPassword());
                }
                ps.setString(index++, account.getType());
                ps.setString(index++, account.getRole());
                ps.setString(index++, account.getStatus());
                ps.setInt(index, account.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw translate("update account", e);
        }
    }

    /** Updates only the status column. Returns false when no row was updated. */
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE account SET status = ? WHERE id = ?";
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw translate("update account status", e);
        }
    }

    /** All codes from account_type_domain. */
    public List<String> listAccountTypes() {
        return listSingleColumn("SELECT code FROM account_type_domain ORDER BY code");
    }

    /** All codes from account_role_domain. */
    public List<String> listAccountRoles() {
        return listSingleColumn("SELECT code FROM account_role_domain ORDER BY code");
    }

    /** All valid (type, role) pairs from account_type_role_domain. */
    public List<AccountTypeRole> listAccountTypeRoles() {
        String sql = "SELECT account_type, account_role FROM account_type_role_domain ORDER BY account_type, account_role";
        List<AccountTypeRole> combos = new ArrayList<>();
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    combos.add(new AccountTypeRole(rs.getString("account_type"), rs.getString("account_role")));
                }
            }
        } catch (SQLException e) {
            throw translate("list type/role combinations", e);
        }
        return combos;
    }

    /** True when the code exists in account_type_domain. */
    public boolean accountTypeExists(String type) {
        return exists("SELECT 1 FROM account_type_domain WHERE code = ?", type);
    }

    /** True when the code exists in account_role_domain. */
    public boolean accountRoleExists(String role) {
        return exists("SELECT 1 FROM account_role_domain WHERE code = ?", role);
    }

    /** True when the (type, role) pair exists in account_type_role_domain. */
    public boolean accountTypeRoleExists(String type, String role) {
        String sql = "SELECT 1 FROM account_type_role_domain WHERE account_type = ? AND account_role = ?";
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, type);
                ps.setString(2, role);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw translate("validate type/role combination", e);
        }
    }

    /**
     * Returns rows from any profile table that references the account id.
     * The schema allows the same account to appear in more than one profile
     * table, so a list is returned and detail.jsp renders each match.
     */
    public List<AccountProfile> findProfilesByAccountId(int accountId) {
        List<AccountProfile> profiles = new ArrayList<>();
        AccountProfile student = findStudentProfile(accountId);
        if (student != null) {
            profiles.add(student);
        }
        AccountProfile staff = findStaffProfile(accountId);
        if (staff != null) {
            profiles.add(staff);
        }
        AccountProfile partnerStaff = findPartnerStaffProfile(accountId);
        if (partnerStaff != null) {
            profiles.add(partnerStaff);
        }
        return profiles;
    }

    private AccountProfile findStudentProfile(int accountId) {
        String sql = "SELECT code, name, status, email FROM student WHERE account_id = ?";
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        AccountProfile profile = new AccountProfile();
                        profile.setProfileType("student");
                        profile.setCode(rs.getString("code"));
                        profile.setName(rs.getString("name"));
                        profile.setStatus(rs.getString("status"));
                        profile.setEmail(rs.getString("email"));
                        return profile;
                    }
                }
            }
        } catch (SQLException e) {
            throw translate("find student profile", e);
        }
        return null;
    }

    private AccountProfile findStaffProfile(int accountId) {
        String sql = "SELECT code, name, status FROM staff WHERE account_id = ?";
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        AccountProfile profile = new AccountProfile();
                        profile.setProfileType("staff");
                        profile.setCode(rs.getString("code"));
                        profile.setName(rs.getString("name"));
                        profile.setStatus(rs.getString("status"));
                        return profile;
                    }
                }
            }
        } catch (SQLException e) {
            throw translate("find staff profile", e);
        }
        return null;
    }

    private AccountProfile findPartnerStaffProfile(int accountId) {
        String sql = "SELECT code, name, position, status FROM partner_staff WHERE account_id = ?";
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        AccountProfile profile = new AccountProfile();
                        profile.setProfileType("partner_staff");
                        profile.setCode(rs.getString("code"));
                        profile.setName(rs.getString("name"));
                        profile.setStatus(rs.getString("status"));
                        profile.setPosition(rs.getString("position"));
                        return profile;
                    }
                }
            }
        } catch (SQLException e) {
            throw translate("find partner_staff profile", e);
        }
        return null;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private List<String> listSingleColumn(String sql) {
        List<String> values = new ArrayList<>();
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    values.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw translate("list domain values", e);
        }
        return values;
    }

    private boolean exists(String sql, String value) {
        try (DBContext ctx = new DBContext()) {
            Connection conn = requireConnection(ctx);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, value);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw translate("domain existence check", e);
        }
    }

    private static Account mapAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getInt("id"));
        account.setUsername(rs.getString("username"));
        account.setType(rs.getString("type"));
        account.setRole(rs.getString("role"));
        account.setStatus(rs.getString("status"));
        return account;
    }

    private static Connection requireConnection(DBContext ctx) {
        Connection conn = ctx.getConnection();
        if (conn == null) {
            throw new DataAccessException("Database connection unavailable.");
        }
        return conn;
    }

    private static void bindStrings(PreparedStatement ps, List<String> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setString(i + 1, params.get(i));
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Converts a SQLException into DataAccessException. Unique-constraint
     * violations (SQL Server error 2601/2627) are flagged so the service can
     * surface the friendly "Username already exists" message.
     */
    private static DataAccessException translate(String operation, SQLException e) {
        int code = e.getErrorCode();
        String state = e.getSQLState();
        boolean duplicate = code == 2601 || code == 2627 || "23000".equalsIgnoreCase(state);
        LOGGER.log(Level.SEVERE, operation + " failed", e);
        return new DataAccessException("Database error during " + operation + ".", e, duplicate);
    }
}