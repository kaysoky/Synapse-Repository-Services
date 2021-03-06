package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupInt;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOUserGroupDAOImpl implements UserGroupDAO {

	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private List<UserGroupInt> bootstrapUsers;
	
	private static final String ID_PARAM_NAME = "id";
	private static final String NAME_PARAM_NAME = "name";
	private static final String IS_INDIVIDUAL_PARAM_NAME = "isIndividual";
	private static final String ETAG_PARAM_NAME = "etag";
	
	private static final String SELECT_BY_NAME_AND_IS_INDIVID_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_NAME+"=:"+NAME_PARAM_NAME+
			" AND "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME;
	
	private static final String SELECT_BY_NAME_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_NAME+"=:"+NAME_PARAM_NAME;
	
	private static final String SELECT_MULTI_BY_NAME_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_NAME+" IN (:"+NAME_PARAM_NAME+")";

	private static final String SELECT_MULTI_BY_PRINCIPAL_IDS = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+" IN (:"+ID_PARAM_NAME+")";
	
	private static final String SELECT_BY_IS_INDIVID_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME;
	
	private static final String SELECT_BY_IS_INDIVID_SQL_PAGINATED = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_BY_IS_INDIVID_OMITTING_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME+
			" AND "+SqlConstants.COL_USER_GROUP_NAME+" NOT IN (:"+NAME_PARAM_NAME+")";
	
	private static final String SELECT_BY_IS_INDIVID_OMITTING_SQL_PAGINATED = 
			SELECT_BY_IS_INDIVID_OMITTING_SQL+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_ALL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP;
	
	private static final String SELECT_ETAG_AND_LOCK_ROW_BY_ID = 
			"SELECT "+SqlConstants.COL_USER_GROUP_E_TAG+" FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+"=:"+ID_PARAM_NAME+
			" FOR UPDATE";
	
	private static final String UPDATE_ETAG_LIST = 
			"UPDATE "+SqlConstants.TABLE_USER_GROUP+
			" SET "+SqlConstants.COL_USER_GROUP_E_TAG+"=:"+ETAG_PARAM_NAME+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+"=:"+ID_PARAM_NAME;

	private static final String SQL_COUNT_USER_GROUPS = "SELECT COUNT("+COL_USER_GROUP_ID+") FROM "+TABLE_USER_GROUP + " WHERE "+COL_USER_GROUP_ID+"=:"+ID_PARAM_NAME;

	private static final RowMapper<DBOUserGroup> userGroupRowMapper = (new DBOUserGroup()).getTableMapping();
	
	
	/**
	 * This is injected by Spring
	 * @param bootstrapUsers
	 */
	public void setBootstrapUsers(List<UserGroupInt> bootstrapUsers) {
		this.bootstrapUsers = bootstrapUsers;
	}

	@Override
	public List<UserGroupInt> getBootstrapUsers() {
		return bootstrapUsers;
	}

	@Override
	public UserGroup findGroup(String name, boolean isIndividual)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME_PARAM_NAME, name);
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		List<DBOUserGroup> ugs = simpleJdbcTemplate.query(SELECT_BY_NAME_AND_IS_INDIVID_SQL, userGroupRowMapper, param);
		if (ugs.size()>1) throw new DatastoreException("Expected 0-1 UserGroups but found "+ugs.size());
		if (ugs.size()==0) return null;
		UserGroup dto = new UserGroup();
		UserGroupUtils.copyDboToDto(ugs.iterator().next(), dto);
		return dto;
	}

	@Override
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupName)
			throws DatastoreException {
		Map<String, UserGroup> dtos = new HashMap<String, UserGroup>();
		if (groupName.isEmpty()) return dtos;
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME_PARAM_NAME, groupName);	
		try {
			List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_MULTI_BY_NAME_SQL, userGroupRowMapper, param);
			
			List<UserGroup> listDtos = new ArrayList<UserGroup>();
			UserGroupUtils.copyDboToDto(dbos, listDtos);
			for (UserGroup dto : listDtos) {
				dtos.put(dto.getName(), dto);
			}
			return dtos;
		} catch (Exception e) {
			throw new DatastoreException("'getGroupsByNames' failed for group list: "+groupName, e);
		}
	}

	@Override
	public Collection<UserGroup> getAll(boolean isIndividual)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_BY_IS_INDIVID_SQL, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;
	}
	
	@Override
	public long getCount()  throws DatastoreException {
		return basicDao.getCount(DBOUserGroup.class);
	}

	@Override
	public Collection<UserGroup> getAllExcept(boolean isIndividual, Collection<String> groupNamesToOmit) throws DatastoreException {
		// the SQL will be invalid for an empty list, so we 'divert' that case:
		if (groupNamesToOmit.isEmpty()) return getAll(isIndividual);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		param.addValue(NAME_PARAM_NAME, groupNamesToOmit);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_BY_IS_INDIVID_OMITTING_SQL, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;
	}
	
	@Override
	public List<UserGroup> getInRange(long fromIncl, long toExcl,
			boolean isIndividual) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		param.addValue(OFFSET_PARAM_NAME, fromIncl);
		long limit = toExcl - fromIncl;
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_BY_IS_INDIVID_SQL_PAGINATED, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;
	}

	@Override
	public List<UserGroup> getInRangeExcept(long fromIncl, long toExcl,
			boolean isIndividual, Collection<String> groupNamesToOmit) throws DatastoreException {
		// the SQL will be invalid for an empty list, so we 'divert' that case:
		if (groupNamesToOmit.isEmpty()) return getInRange(fromIncl, toExcl, isIndividual);
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		param.addValue(OFFSET_PARAM_NAME, fromIncl);
		long limit = toExcl - fromIncl;
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(NAME_PARAM_NAME, groupNamesToOmit);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_BY_IS_INDIVID_OMITTING_SQL_PAGINATED, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;
	}

	public DBOUserGroup findGroup(String name) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME_PARAM_NAME, name);	
		List<DBOUserGroup> ugs = simpleJdbcTemplate.query(SELECT_BY_NAME_SQL, userGroupRowMapper, param);
		if (ugs.size()>1) throw new DatastoreException("Expected 0-1 UserGroups but found "+ugs.size());
		if (ugs.size()==0) return null;
		return ugs.iterator().next();
	}
	
	@Override
	public boolean doesPrincipalExist(String name) {
		try {
			return null!=findGroup(name);
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public boolean deletePrincipal(String name) {
		
		try {
			DBOUserGroup ug = findGroup(name);
			if (ug==null) return false;
			
			// Send a DELETE message
			transactionalMessenger.sendMessageAfterCommit("" + ug.getId(), ObjectType.PRINCIPAL, ug.getEtag(), ChangeType.DELETE);
			
			delete(ug.getId().toString());
			return true;
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			return false;
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(UserGroup dto) throws DatastoreException,
			InvalidModelException {
		// The public version unconditionally clears the ID so a new one will be assigned
		dto.setId(null);
		DBOUserGroup dbo = createPrivate(dto);
		
		// Send a CREATE message
		// Note: This message cannot be sent in the createPrivate method because
		// bootstrapping is not transactional when called by the Spring initializer 
		transactionalMessenger.sendMessageAfterCommit("" + dbo.getId(), ObjectType.PRINCIPAL, dbo.getEtag(), ChangeType.CREATE);
		
		return dbo.getId().toString();
	}

	/**
	 * This will not clear the ID like the public method.
	 * This allows us to boostrap users with set IDs.
	 * @param dto
	 * @return
	 */
	private DBOUserGroup createPrivate(UserGroup dto) {
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		// If the create is successful, it should have a new etag
		dbo.setEtag(UUID.randomUUID().toString());
		// Bootstraped users will have IDs already assigned.
		if(dbo.getId() == null){
			// We allow the ID generator to create all other IDs
			dbo.setId(idGenerator.generateNewId(TYPE.PRINCIPAL_ID));
		}
		
		try {
			dbo = basicDao.createNew(dbo);
		} catch (Exception e) {
			throw new DatastoreException("id=" + dbo.getId() + " name="+dto.getName(), e);
		}
		
		return dbo;
	}

	public boolean doesIdExist(Long id) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(ID_PARAM_NAME, id);
		try{
			long count = simpleJdbcTemplate.queryForLong(SQL_COUNT_USER_GROUPS, parameters);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}
	
	@Override
	public UserGroup get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		DBOUserGroup dbo;
		try {
			dbo = basicDao.getObjectByPrimaryKey(DBOUserGroup.class, param);
		} catch (NotFoundException e) {
			// Rethrow the basic DAO's generic error message
			throw new NotFoundException("Principal (" + id + ") does not exist", e);
		}
		UserGroup dto = new UserGroup();
		UserGroupUtils.copyDboToDto(dbo, dto);
		return dto;
	}
	
	@Override
	public List<UserGroup> get(List<String> ids) throws DatastoreException {
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		if (ids.isEmpty()) {
			return dtos;
		}
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, ids);
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_MULTI_BY_PRINCIPAL_IDS, userGroupRowMapper, param);
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;
	}

	@Override
	public Collection<UserGroup> getAll() throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		List<DBOUserGroup> dbos = simpleJdbcTemplate.query(SELECT_ALL, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;

	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void update(UserGroup dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		
		// If the update is successful, it should have a new etag
		dbo.setEtag(UUID.randomUUID().toString());
		
		// Send a UPDATE message
		transactionalMessenger.sendMessageAfterCommit("" + dbo.getId(), ObjectType.PRINCIPAL, dbo.getEtag(), ChangeType.UPDATE);

		basicDao.update(dbo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		basicDao.deleteObjectByPrimaryKey(DBOUserGroup.class, param);
	}
	
	/**
	 * This is called by Spring after all properties are set
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void bootstrapUsers() throws Exception {
		// Reserver an ID well above the current
		idGenerator.reserveId(3318977l, TYPE.PRINCIPAL_ID);
		
		// Boot strap all users and groups
		if (this.bootstrapUsers == null) {
			throw new IllegalArgumentException("bootstrapUsers cannot be null");
		}
		
		// For each one determine if it exists, if not create it
		for (UserGroupInt ug: this.bootstrapUsers) {
			if (ug.getId() == null) {
				throw new IllegalArgumentException("Bootstrap users must have an id");
			}
			if (ug.getName() == null) {
				throw new IllegalArgumentException("Bootstrap users must have a name");
			}
			
			Long id = Long.parseLong(ug.getId());
			if (!this.doesIdExist(id)) {
				UserGroup newUg = new UserGroup();
				newUg.setId(ug.getId());
				newUg.setName(ug.getName());
				newUg.setIsIndividual(ug.getIsIndividual());
				this.createPrivate(newUg);
			}
		}
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String getEtagForUpdate(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		return simpleJdbcTemplate.queryForObject(SELECT_ETAG_AND_LOCK_ROW_BY_ID, 
				new RowMapper<String>() {
					@Override
					public String mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						return rs.getString(SqlConstants.COL_USER_GROUP_E_TAG);
					}
				}, param);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void touch(Long principalId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(ETAG_PARAM_NAME, UUID.randomUUID().toString());
		simpleJdbcTemplate.update(UPDATE_ETAG_LIST, param);
	}
}
