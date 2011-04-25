package com.ifsoft.iftalk.plugin.voicebridge;

import com.ifsoft.iftalk.plugin.tsc.*;
import org.apache.log4j.Logger;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SiteDao extends AbstractSiteDao {

	private static final String GET_SITEBYID = "SELECT * FROM ofvoicebridge WHERE siteID = ? ";

	private static final String GET_ALLSITE = "SELECT * FROM ofvoicebridge";

	private static final String INSERT_SITE =
		"INSERT INTO ofvoicebridge (siteID, name, privateHost, publicHost, defaultProxy, defaultExten) VALUES (?,?,?,?,?,?)";

	private static final String UPDATE_SITE =
		"UPDATE ofvoicebridge SET name = ?, privateHost=?, publicHost=?, defaultProxy=?, defaultExten=? WHERE siteID=?";

	private static final String DELETE_SITE = "DELETE FROM ofvoicebridge WHERE siteID = ?";

	private RedfirePlugin plugin;

	public SiteDao(RedfirePlugin plugin)
	{
		this.plugin = plugin;
	}

	public Site getSiteByID(long  siteID) {
		Site site = null;
		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;

		try {

			con = DbConnectionManager.getConnection();
			psmt = con.prepareStatement(GET_SITEBYID);
			psmt.setLong(1, siteID);
			rs = psmt.executeQuery();

			if (rs.next()) {
				site = read(rs);
			}



		} catch (SQLException e) {
			Log.error(e.getMessage(), e);
		} finally {
			DbConnectionManager.closeConnection(rs, psmt, con);
		}

		return site;
	}



	private static Site read(ResultSet rs) {
		Site site = null;
		try {

			long siteID = rs.getLong("siteID");
			String name = rs.getString("name");
			String privateHost = rs.getString("privateHost");
			String publicHost = rs.getString("publicHost");
			String defaultProxy = rs.getString("defaultProxy");
			String defaultExten = rs.getString("defaultExten");

			site = new Site();
			site.setSiteID(siteID);
			site.setName(name);
			site.setPrivateHost(privateHost);
			site.setPublicHost(publicHost);
			site.setDefaultProxy(defaultProxy);
			site.setDefaultExten(defaultExten);

        }
		catch (SQLException e) {
			Logger.getLogger("SiteDao").error(e.getMessage(), e);
		}
		return site;
	}

	public void insert(Site site) throws SQLException {

		Log.info("Adding new site " + site.getName());
		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		//site.setSiteID(SequenceManager.nextID(site));
		try {
			con = DbConnectionManager.getConnection();
			psmt = con.prepareStatement(INSERT_SITE);
			psmt.setLong(1, site.getSiteID());
			psmt.setString(2, site.getName());
			psmt.setString(3, site.getPrivateHost());
			psmt.setString(4, site.getPublicHost());
			psmt.setString(5, site.getDefaultProxy());
			psmt.setString(6, site.getDefaultExten());

            psmt.executeUpdate();


		} catch (SQLException e) {
			Log.error(e.getMessage(), e);
			throw new SQLException(e.getMessage());
		} finally {
			DbConnectionManager.closeConnection(rs, psmt, con);
		}

		if (plugin != null) plugin.siteAdded(site);
	}

	public void update(Site site) throws SQLException {

		Log.info("Updating site " + site.getName());
		Connection con = null;
		PreparedStatement psmt = null;
		try {
			con = DbConnectionManager.getConnection();
			psmt = con.prepareStatement(UPDATE_SITE);
			psmt.setString(1, site.getName());
			psmt.setString(2, site.getPrivateHost());
			psmt.setString(3, site.getPublicHost());
			psmt.setString(4, site.getDefaultProxy());
			psmt.setString(5, site.getDefaultExten());

			psmt.setLong(6, site.getSiteID());
            psmt.executeUpdate();

		} catch (SQLException e) {
			Log.error(e.getMessage(), e);
			throw new SQLException(e.getMessage());
		} finally {
			DbConnectionManager.closeConnection(psmt, con);
		}

		if (plugin != null) plugin.siteUpdated(site);
	}

	public void remove(long siteID) {
		Site site = this.getSiteByID(siteID);
		if (plugin != null) plugin.siteRemoved(site);

		Log.info("Deleteing site " + siteID);
		Connection con = null;
		PreparedStatement psmt = null;

		try {
			con = DbConnectionManager.getConnection();
			psmt = con.prepareStatement(DELETE_SITE);
            psmt.setLong(1, siteID);
			psmt.executeUpdate();
            psmt.close();

        } catch (SQLException e) {
			Log.error(e.getMessage(), e);
		} finally {
			DbConnectionManager.closeConnection(psmt, con);
		}
	}

	public Collection<Site> getSites() {

		List<Site> sites = new ArrayList<Site>();
		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = DbConnectionManager.createScrollablePreparedStatement(con, GET_ALLSITE);
			ResultSet rs = pstmt.executeQuery();
			//DbConnectionManager.setFetchSize(rs, startIndex + numResults);
			//DbConnectionManager.scrollResultSet(rs, startIndex);
			//int count = 0;
			while (rs.next()) {
				sites.add(read(rs));
				//count++;
			}
			rs.close();
		}
		catch (SQLException e) {
			Log.error(e);
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (Exception e) {
				Log.error(e);
			}
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception e) {
				Log.error(e);
			}
		}
		return sites;
	}

	public static Collection<Site> getSites2() {

		List<Site> sites = new ArrayList<Site>();
		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = DbConnectionManager.createScrollablePreparedStatement(con, GET_ALLSITE);
			ResultSet rs = pstmt.executeQuery();
			//DbConnectionManager.setFetchSize(rs, startIndex + numResults);
			//DbConnectionManager.scrollResultSet(rs, startIndex);
			//int count = 0;
			while (rs.next()) {
				sites.add(read(rs));
				//count++;
			}
			rs.close();
		}
		catch (SQLException e) {
			Logger.getLogger("SiteDao").error(e);
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (Exception e) {
				Logger.getLogger("SiteDao").error(e);
			}
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception e) {
				Logger.getLogger("SiteDao").error(e);
			}
		}
		return sites;
	}

}
