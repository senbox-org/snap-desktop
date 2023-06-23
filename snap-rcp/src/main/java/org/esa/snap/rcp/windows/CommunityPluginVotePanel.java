package org.esa.snap.rcp.windows;

import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import javax.net.ssl.HttpsURLConnection;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.esa.snap.core.util.SystemUtils;

/**
 * Panel containing the Community Plugin vote dialog
 * 
 * @author Lucian Barbulescu
 *
 */
public class CommunityPluginVotePanel extends JPanel {

	/**
	 * Generated Serial ID
	 */
	private static final long serialVersionUID = -3282916392722244406L;

	/** JSON deserializer. */
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * The associated plugin name;
	 */
	private String name;

	/**
	 * The associated plugin version;
	 */
	private String version;

	/**
	 * The stars labels
	 */
	private JLabel[] stars = new JLabel[] { new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel() };

	/**
	 * The vote text
	 */
	private JLabel votesText = new JLabel();

	/**
	 * The "vote" button
	 */
	private JButton vote = new JButton();
	
	/**
	 * Constructor.
	 */
	public CommunityPluginVotePanel() {
		// create the executor
		buildUI();
		addHierarchyListener(new HierarchyListener() {

			@Override
			public void hierarchyChanged(HierarchyEvent e) {

				if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0x0l) {
					if (name == null || version == null) {
						return;
					}
					Container p = e.getChangedParent();
					while (p != null) {
						if (p.toString().contains("org.netbeans.modules.autoupdate.ui.UnitTab")) {
							try {
								final ExecutorService executor = Executors.newSingleThreadExecutor();
								final Method m = p.getClass().getMethod("getHelpId");
								final Object result = m.invoke(p);
								final AsyncPluginReviewRetriever retriever = new AsyncPluginReviewRetriever(name, version, result.toString(), executor);
								retriever.executeAsync();

								break;
							} catch (NoSuchMethodException ex) {
								// not here
							} catch (SecurityException ex) {
					            SystemUtils.LOG.severe("Access error");
							} catch (IllegalAccessException ex) {
								SystemUtils.LOG.severe("Invokation error");
							} catch (IllegalArgumentException ex) {
								SystemUtils.LOG.severe("Invokation error");
							} catch (InvocationTargetException ex) {
								SystemUtils.LOG.severe("Invokation error");
							}

						}

						p = p.getParent();
					}
				}

			}
		});
	}

	/**
	 * Invoked when the plugin data was obtained from the server
	 * 
	 * @param plugin the plugin data or null if none found
	 * @param helpId the help ID of the page where the dialog is opened.
	 */
	void onPluginDataRetrieved(final CommunityPluginData plugin, final String helpId) {
		if (plugin != null) {
			// update stars and review count.
			for (int i = 0; i < 5; i++) {
				this.stars[i].setVisible(true);
				if ((i + 1) <= plugin.getRate()) {
					this.stars[i].setIcon(
							new ImageIcon(this.getClass().getResource("/org/esa/snap/rcp/icons/FullStar.png")));
				} else if (i < plugin.getRate()) {
					this.stars[i].setIcon(
							new ImageIcon(this.getClass().getResource("/org/esa/snap/rcp/icons/HalfStar.png")));
				} else {
					this.stars[i].setIcon(
							new ImageIcon(this.getClass().getResource("/org/esa/snap/rcp/icons/NoStar.png")));
				}
			}

			this.votesText.setText(plugin.getRate() + " / 5.0 (" + plugin.getCount() + " reviews)");
			
			// Enable the review button only for Installed plugins
			if (helpId.contains("INSTALLED")) {
				vote.setVisible(true);
			}
		} else {
			this.votesText.setText("The plugin " + name + ", version " + version + " is not available online!");
		}
	}

	/**
	 * Build the UI of the component
	 */
	private void buildUI() {
		setSize(350, 40);
		setLayout(new GridBagLayout());
		setBackground(Color.WHITE);

		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.insets = new Insets(1, 0, 1, 1);

		for (int i = 0; i < 5; i++) {
			c.gridx = i;
			this.add(this.stars[i], c);
			this.stars[i].setIcon(
					new ImageIcon(this.getClass().getResource("/org/esa/snap/rcp/icons/NoStar.png")));
			this.stars[i].setVisible(false);
		}

		c.gridx = 5;
		c.weightx = 1.0;
		this.add(this.votesText, c);
		this.votesText.setText("Loading Plugin Reviews Data ...");

		c.gridx = 6;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.EAST;
		this.add(this.vote, c);
		this.vote.addActionListener(l -> {
			try {
				final URI uri = new URI("https://step.esa.int/main/snap-community-plugins/");
				Desktop d = Desktop.getDesktop();
				d.browse(uri);
			} catch (URISyntaxException e) {
				// invalid URL
			} catch (IOException e) {
				// cannot open browser
			}
		});
		this.vote.setVisible(false);
		this.vote.setText("Add Review");
	}

	/**
	 * Set the plugin name.
	 * 
	 * @param name the plugin name
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Set the plugin version.
	 * 
	 * @param name the plugin version
	 */
	public void setVersion(final String version) {
		this.version = version;
	}

	/**
	 * Asynchronous retriever for plugin review data.
	 * 
	 * @author Lucian Barbulescu
	 */
	private class AsyncPluginReviewRetriever {
		
		/** Name of the plugin. */
		private final String pluginName;
		/** Version of the plugin. */
		private final String pluginVersion;
		/** The help id of the page which contains this dialog. */
		private final String helpId;
		/** Service used to obtain the plugin's review data asynchronously. */
		private final ExecutorService executor;
		
		/**
		 * Constructor.
		 *
		 * @param pluginName
		 * @param pluginVersion
		 * @param helpId
		 * @param executor
		 */
		public AsyncPluginReviewRetriever(String pluginName, String pluginVersion, String helpId, ExecutorService executor) {
			this.pluginName = pluginName;
			this.pluginVersion = pluginVersion;
			this.helpId = helpId;
			this.executor = executor;
		}
		
		/**
		 * Perform the asynchronous execution.
		 */
		void executeAsync() {
			this.executor.submit(() -> 
		    {
		    	final CommunityPluginResponse pluginsData = getPluginsInfo();
				// check if the success field is true.
				if (pluginsData != null && pluginsData.isSuccess()) {
					for (final CommunityPluginData plugin : pluginsData.getPlugins().getResults()) {
						final String pluginName = plugin.getName();
						final String pluginVersion = plugin.getVersion();
						if (name.equals(pluginName) && version.equals(pluginVersion)) {
							onPluginDataRetrieved(plugin, helpId);
							return;
						}
					}
				}

				onPluginDataRetrieved(null, helpId);
		    });
		}
		
		/**
		 * Get the plugins info.
		 * 
		 * @return
		 */
		private CommunityPluginResponse getPluginsInfo() {
			HttpsURLConnection c = null;
			CommunityPluginResponse response = null;
			try {
				URL u = new URL("https://step.esa.int/communityplugins/api/plugins/all/");
				c = (HttpsURLConnection) u.openConnection();
				c.setRequestMethod("GET");
				c.setRequestProperty("Content-length", "0");
				c.setUseCaches(false);
				c.setAllowUserInteraction(false);
				c.setConnectTimeout(5000);
				c.setReadTimeout(5000);
				c.connect();
				int status = c.getResponseCode();

				if (status == 200 || status == 201) {
					BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						sb.append(line);
					}
					br.close();

					response = OBJECT_MAPPER.readValue(sb.toString(), new TypeReference<CommunityPluginResponse>() {});
				}

			} catch (IOException ex) {
				SystemUtils.LOG.severe("Error reading server data. ");
				SystemUtils.LOG.info("Error message: " + ex.getMessage());
			} finally {
				if (c != null) {
					try {
						c.disconnect();
					} catch (Exception ex) {

					}
				}
			}
			return response;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class CommunityPluginResponse {
		/** Result of the call. */
		private boolean success;
		/** Details related to the community plugins */
		private CommunityPluginList plugins;

		/**
		 * Get the current value of the success.
		 *
		 * @return the success
		 */
		public boolean isSuccess() {
			return this.success;
		}

		/**
		 * Set a new value for the success.
		 *
		 * @param success the success to set
		 */
		public void setSuccess(boolean success) {
			this.success = success;
		}

		/**
		 * Get the current value of the plugins.
		 *
		 * @return the plugins
		 */
		public CommunityPluginList getPlugins() {
			return this.plugins;
		}

		/**
		 * Set a new value for the plugins.
		 *
		 * @param plugins the plugins to set
		 */
		public void setPlugins(CommunityPluginList plugins) {
			this.plugins = plugins;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class CommunityPluginList {
		/** The list of plugins. */
		private List<CommunityPluginData> results;

		/**
		 * Get the current value of the results.
		 *
		 * @return the results
		 */
		public List<CommunityPluginData> getResults() {
			return this.results;
		}

		/**
		 * Set a new value for the results.
		 *
		 * @param results the results to set
		 */
		public void setResults(List<CommunityPluginData> results) {
			this.results = results;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class CommunityPluginData {
		@JsonProperty("plugin_id")
		private Integer id;

		@JsonProperty("plugin_name")
		private String name;

		@JsonProperty("plugin_version")
		private String version;

		@JsonProperty("avg_rate")
		private Double rate;

		@JsonProperty("count_rate")
		private Integer count;

		/**
		 * Get the current value of the id.
		 *
		 * @return the id
		 */
		public Integer getId() {
			return this.id;
		}

		/**
		 * Set a new value for the id.
		 *
		 * @param id the id to set
		 */
		public void setId(Integer id) {
			this.id = id;
		}

		/**
		 * Get the current value of the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Set a new value for the name.
		 *
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Get the current value of the version.
		 *
		 * @return the version
		 */
		public String getVersion() {
			return this.version;
		}

		/**
		 * Set a new value for the version.
		 *
		 * @param version the version to set
		 */
		public void setVersion(String version) {
			this.version = version;
		}

		/**
		 * Get the current value of the rate.
		 *
		 * @return the rate
		 */
		public Double getRate() {
			return this.rate;
		}

		/**
		 * Set a new value for the rate.
		 *
		 * @param rate the rate to set
		 */
		public void setRate(Double rate) {
			this.rate = rate;
		}

		/**
		 * Get the current value of the count.
		 *
		 * @return the count
		 */
		public Integer getCount() {
			return this.count;
		}

		/**
		 * Set a new value for the count.
		 *
		 * @param count the count to set
		 */
		public void setCount(Integer count) {
			this.count = count;
		}
	}
}
