package org.geosde.cassandra;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geosde.core.data.ContentDataStore;
import org.geosde.core.data.ContentEntry;
import org.geosde.core.data.ContentFeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.datastax.driver.core.ColumnMetadata;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.TableMetadata;
import com.vividsolutions.jts.geom.Geometry;

public class CassandraDataStore extends ContentDataStore {

	private CassandraConnector connector;

	private String catalog_name;

	/**
	 * The native SRID associated to a certain descriptor TODO: qualify this key
	 * with 'org.geotools.jdbc'
	 */
	public static final String CASSANDRA_NATIVE_SRID = "nativeSRID";

	public CassandraDataStore() {
		connector = new CassandraConnector();
	}

	//
	// Property accessors
	//

	public String getCatalog_name() {
		return catalog_name;
	}

	public void setCatalog_name(String catalog_name) {
		this.catalog_name = catalog_name;
	}

	//
	// API Implementation
	//
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		return new CassandraFeatureSource(connector.getSession(), entry, Query.ALL);
	}

	@Override
	protected List<Name> createTypeNames() throws IOException {
		Session session = connector.getSession();
		List<Name> typeNames = new ArrayList<>();
		String namespace = getNamespaceURI();
		SimpleStatement statement = new SimpleStatement("SELECT workspace_name,layer_name FROM catalog.layers");
		ResultSet rs = session.execute(statement);

		for (Row row : rs) {
			String workspace_name = row.getString("workspace_name");
			String layer_name = row.getString("layer_name");
			if (namespace.equals(workspace_name))
				typeNames.add(new NameImpl(workspace_name, layer_name));
		}
		session.close();
		return typeNames;
	}

	@Override
	public void createSchema(SimpleFeatureType featureType) throws IOException {
		Session session = connector.getSession();
		String catalog_name = "usa";
		String workspace_name = getNamespaceURI();
		String layer_name = featureType.getTypeName();
		Date cdate = new Date();
		String owner = "xiaofei";
		String geometry_type = featureType.getGeometryDescriptor().getType().getName().getLocalPart();
		String geometry_column = "the_geom";
		int srid = getSRID(featureType);
		double minx = 0;
		double miny = 0;
		double maxx = 0;
		double maxy = 0;
		String keywords = "";

		SimpleStatement statement = new SimpleStatement(
				"INSERT INTO catalog.layers (catalog_name,workspace_name,layer_name,cdate,owner,geometry_type,geometry_column,srid,minx,miny,maxx,maxy,keywords) "
						+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?);",
				catalog_name, workspace_name, layer_name, cdate.getTime(), owner, geometry_type, geometry_column, srid,
				minx, miny, maxx, maxy, keywords);
		session.execute(statement);

		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE IF NOT EXISTS " + workspace_name + "."
				+ featureType.getName().toString().replace(".", "_") + " (");
		builder.append("cell_id text,");
		builder.append("epoch text,");
		builder.append("pos bigint,");
		builder.append("timestamp bigint,");
		builder.append("fid uuid,");
		List<AttributeDescriptor> attrDes = featureType.getAttributeDescriptors();
		List<String> col_items = new ArrayList<>();
		for (AttributeDescriptor attr : attrDes) {
			String col_name = attr.getLocalName();
			Class type = attr.getType().getBinding();
			builder.append(col_name + " " + CassandraTypeConvertor.TYPE_TO_CA_MAP.get(type).getName().toString() + ",");
		}
		builder.append("PRIMARY KEY ((cell_id,epoch),pos,timestamp,fid)");
		builder.append(");");
		session.execute(builder.toString());
		session.close();
	}

	@Override
	public SimpleFeatureType getSchema(Name name) throws IOException {
		String catalog_name = getCatalog_name();
		String workspace_name = getNamespaceURI();
		KeyspaceMetadata keyspaceMetadata = connector.getCluster().getMetadata().getKeyspace(getNamespaceURI());
		TableMetadata table = keyspaceMetadata.getTable(name.toString());
		return getSchema(catalog_name,workspace_name,name, table);
	}

	public SimpleFeatureType getSchema(String catalog_name,String workspace_name,Name name, TableMetadata table) {
		Session session = connector.getSession();
		SimpleStatement statement = new SimpleStatement(
				"SELECT * FROM catalog.layers where catalog_name=? and workspace_name=? and layer_name=?;",catalog_name,workspace_name,name.getLocalPart());
		ResultSet rs=session.execute(statement);
		Row row=rs.one();
		int srid=row.getInt("srid");
		List<ColumnMetadata> columns = table.getColumns();
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName(name);
		AttributeTypeBuilder attrTypeBuilder = new AttributeTypeBuilder();
		for (ColumnMetadata cm : columns) {
			String cname = cm.getName();
			Class binding = CassandraTypeConvertor.CA_MAP_TO_TYPE.get(cm.getType());
			if (!cm.getName().equals("cell_id") && !cm.getName().equals("epoch") && !cm.getName().equals("pos")
					&& !cm.getName().equals("timestamp") && !cm.getName().equals("fid")) {
				if (Geometry.class.isAssignableFrom(binding)) {
					attrTypeBuilder.binding(binding);
					CoordinateReferenceSystem wsg84 = null;
					try {
						wsg84 = CRS.decode("EPSG:"+srid);
					} catch (Exception e) {
						e.printStackTrace();
					}
					attrTypeBuilder.setCRS(wsg84);
					builder.add(attrTypeBuilder.buildDescriptor(cname, attrTypeBuilder.buildGeometryType()));
				} else {
					builder.add(attrTypeBuilder.binding(binding).nillable(false).buildDescriptor(cname));
				}
			}
		}
		session.close();
		return builder.buildFeatureType();
	}

	//
	// Internal methods
	//
	/**
	 * Looks up the geometry srs by trying a number of heuristics. Returns -1 if
	 * all attempts at guessing the srid failed.
	 */

	protected int getSRID(SimpleFeatureType featureType) {
		int srid = -1;
		CoordinateReferenceSystem flatCRS = CRS.getHorizontalCRS(featureType.getCoordinateReferenceSystem());
		try {
			Integer candidate = CRS.lookupEpsgCode(flatCRS, false);
			if (candidate != null)
				srid = candidate;
			else
				srid = 4326;
		} catch (Exception e) {
			// ok, we tried...
		}
		return srid;
	}

	protected int getGeometrySRID(Geometry g, AttributeDescriptor descriptor) throws IOException {
		int srid = getDescriptorSRID(descriptor);

		if (g == null) {
			return srid;
		}

		// check for srid in the jts geometry then
		if (srid <= 0 && g.getSRID() > 0) {
			srid = g.getSRID();
		}

		// check if the geometry has anything
		if (srid <= 0 && g.getUserData() instanceof CoordinateReferenceSystem) {
			// check for crs object
			CoordinateReferenceSystem crs = (CoordinateReferenceSystem) g.getUserData();
			try {
				Integer candidate = CRS.lookupEpsgCode(crs, false);
				if (candidate != null)
					srid = candidate;
			} catch (Exception e) {
				// ok, we tried...
			}
		}

		return srid;
	}

	/**
	 * Extracts the eventual native SRID user property from the descriptor,
	 * returns -1 if not found
	 * 
	 * @param descriptor
	 */
	protected int getDescriptorSRID(AttributeDescriptor descriptor) {
		int srid = -1;

		// check if we have stored the native srid in the descriptor (we should)
		if (descriptor.getUserData().get(CassandraDataStore.CASSANDRA_NATIVE_SRID) != null)
			srid = (Integer) descriptor.getUserData().get(CassandraDataStore.CASSANDRA_NATIVE_SRID);
		return srid;
	}
}
