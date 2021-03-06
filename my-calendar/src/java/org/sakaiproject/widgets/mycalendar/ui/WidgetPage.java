
package org.sakaiproject.widgets.mycalendar.ui;

import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesService;

public class WidgetPage extends WebPage {

	private static final long serialVersionUID = 1L;

	@SpringBean(name = "org.sakaiproject.tool.api.ToolManager")
	private ToolManager toolManager;

	@SpringBean(name = "org.sakaiproject.tool.api.SessionManager")
	private SessionManager sessionManager;

	@SpringBean(name = "org.sakaiproject.user.api.PreferencesService")
	private PreferencesService preferencesService;

	@SpringBean(name = "org.sakaiproject.component.api.ServerConfigurationService")
	private ServerConfigurationService serverConfigurationService;

	public WidgetPage() {
		// log.debug("WidgetPage()");
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		// setup the data for the page
		final Label data = new Label("data");
		data.add(new AttributeAppender("data-siteid", getCurrentSiteId()));
		data.add(new AttributeAppender("data-tz", getUserTimeZone().getID()));
		data.add(new AttributeAppender("data-namespace", getNamespace()));

		add(data);
	}

	@Override
	public void renderHead(final IHeaderResponse response) {
		super.renderHead(response);

		final String version = this.serverConfigurationService.getString("portal.cdn.version", "");

		// get the Sakai skin header fragment from the request attribute
		final HttpServletRequest request = (HttpServletRequest) getRequest().getContainerRequest();

		response.render(StringHeaderItem.forString((String) request.getAttribute("sakai.html.head")));
		response.render(OnLoadHeaderItem.forScript("setMainFrameHeight( window.name )"));

		// Tool additions (at end so we can override if required)
		response.render(StringHeaderItem.forString("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"));

		// render jQuery and the Wicket event library
		// Both must be priority so they are emitted into the head
		response.render(new PriorityHeaderItem(JavaScriptHeaderItem.forUrl(String.format("/library/webjars/jquery/1.11.3/jquery.min.js?version=%s", version))));
		response.render(new PriorityHeaderItem(JavaScriptHeaderItem.forUrl(String.format("/my-calendar/scripts/wicket/wicket-event-jquery.min.js?version=%s", version))));
		
		// additional styles (datepicker, this widget etc)
		response.render(CssHeaderItem.forUrl(String.format("/library/webjars/jquery-ui/1.11.3/jquery-ui.min.css?version=%s", version)));
		response.render(CssHeaderItem.forUrl(String.format("/my-calendar/styles/widget-styles.css?version=%s", version)));
		
		// NOTE: All libraries apart from jQuery and Wicket Event must be rendered inline with the application. See WidgetPage.html.
		
	}

	/**
	 * Get the current siteId
	 *
	 * @return
	 */
	private String getCurrentSiteId() {
		try {
			return this.toolManager.getCurrentPlacement().getContext();
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 * Get current user id
	 *
	 * @return
	 */
	private String getCurrentUserId() {
		return this.sessionManager.getCurrentSessionUserId();
	}

	/**
	 * Get a user's timezone from their preferences.
	 *
	 * @param userUuid uuid of the user to get preferences for
	 * @return TimeZone from user preferences or the default timezone of the server if none is set
	 */
	private TimeZone getUserTimeZone() {

		TimeZone timezone;
		final Preferences prefs = this.preferencesService.getPreferences(getCurrentUserId());
		final ResourceProperties props = prefs.getProperties(TimeService.APPLICATION_ID);
		final String tzPref = props.getProperty(TimeService.TIMEZONE_KEY);

		if (StringUtils.isNotBlank(tzPref)) {
			timezone = TimeZone.getTimeZone(tzPref);
		} else {
			timezone = TimeZone.getDefault();
		}

		return timezone;
	}
	
	/**
	 * Get a UUID with dashes removed that we can use as a namespace
	 * @return
	 */
	private String getNamespace() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

}
