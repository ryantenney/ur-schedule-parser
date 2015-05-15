package com.ryantenney.scheduleparser;

import com.ryantenney.scheduleparser.ical4j.UidGenerator;

import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class ScheduleParser {

	private static final DateTimeFormatter DATE_PARSER = new DateTimeFormatterBuilder()
			.appendDayOfWeekShortText()
			.appendLiteral(' ')
			.appendMonthOfYear(2)
			.appendLiteral('/')
			.appendDayOfMonth(2)
			.appendLiteral('/')
			.appendYear(4, 4)
			.toFormatter();

	public static void main(String[] args) throws Throwable {
		String inputFile = args[0];
		String outputFile = args[1];

		//String inputFile = "/Users/ryantenney/Downloads/Med Schedule Employee Re_Preview.html";
		//String outputFile = "output.ics";

		process(inputFile, outputFile);
	}

	public static void process(String inputFile, String outputFile) throws IOException, ValidationException {
		Document html = Jsoup.parse(new File(inputFile), "UTF-8");
		List<Shift> schedule = parseSchedule(html);
		String calendar = generateCalendar(schedule);
		FileUtils.writeStringToFile(new File(outputFile), calendar);
	}

	private static List<Shift> parseSchedule(Document document) {
		final Pattern startCellPattern = Pattern.compile("^Previous Schedule|Week \\d$");

		List<Shift> schedule = new ArrayList<>();
		Elements tableRows = document.select("body > form > table > tbody > tr");
		Iterator<Element> tableRowIter = tableRows.iterator();
		while (tableRowIter.hasNext()) {
			Element tr = tableRowIter.next();
			if (tr.children().isEmpty()) {
				continue;
			}

			Element trFirstChild = tr.child(0);
			if (startCellPattern.matcher(trFirstChild.text()).matches()) {
				Elements weekRow = tr.children();
				Elements timeCodeRow = tableRowIter.next().children();
				Elements npTimeRow = tableRowIter.next().children();

				for (int col = 1, len = weekRow.size(); col < len; col++) {
					String dateCell = trim(weekRow.get(col).text());
					if (dateCell != null) {
						LocalDate date = DATE_PARSER.parseLocalDate(dateCell);

						String timeCodeCell = trim(timeCodeRow.get(col).text());
						if (timeCodeCell != null) {
							LocalDateTime startDate, endDate;
							switch (timeCodeCell) {
								case "D":
									startDate = date.toLocalDateTime(new LocalTime(7, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(15, 0, 0));
									schedule.add(new Shift("Day Shift", startDate, endDate));
									break;

								case "D12":
									startDate = date.toLocalDateTime(new LocalTime(7, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(19, 0, 0));
									schedule.add(new Shift("Day Shift", startDate, endDate));
									break;

								case "N":
									startDate = date.minusDays(1).toLocalDateTime(new LocalTime(23, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(7, 0, 0));
									schedule.add(new Shift("Night Shift", startDate, endDate));
									break;

								case "N12":
									startDate = date.minusDays(1).toLocalDateTime(new LocalTime(19, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(7, 0, 0));
									schedule.add(new Shift("Night Shift", startDate, endDate));
									break;

								case "E":
									startDate = date.toLocalDateTime(new LocalTime(15, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(23, 0, 0));
									schedule.add(new Shift("Evening Shift", startDate, endDate));
									break;

								case "E12":
									startDate = date.toLocalDateTime(new LocalTime(11, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(23, 0, 0));
									schedule.add(new Shift("Evening Shift", startDate, endDate));
									break;

								case "E4A":
									startDate = date.toLocalDateTime(new LocalTime(15, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(19, 0, 0));
									schedule.add(new Shift("Evening Shift", startDate, endDate));
									break;

								case "E4B":
									startDate = date.toLocalDateTime(new LocalTime(19, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(23, 0, 0));
									schedule.add(new Shift("Evening Shift", startDate, endDate));
									break;

								case "DE8":
									startDate = date.toLocalDateTime(new LocalTime(11, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(19, 0, 0));
									schedule.add(new Shift("Day Shift (Late Start)", startDate, endDate));
									break;

								default:
									break;
							}
						}

						String npTimeCell = trim(npTimeRow.get(col).text());
						if (npTimeCell != null) {
							LocalDateTime startDate, endDate;
							switch (npTimeCell) {
								case "UC4":
									startDate = date.toLocalDateTime(new LocalTime(7, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(11, 0, 0));
									schedule.add(new Shift("Unit Council", startDate, endDate));
									break;

								case "WC4":
									startDate = date.toLocalDateTime(new LocalTime(7, 0, 0));
									endDate = date.toLocalDateTime(new LocalTime(11, 0, 0));
									schedule.add(new Shift("Wound Care", startDate, endDate));
									break;

								default:
									break;
							}
						}
					}
				}
			}
		}

		return schedule;
	}

	private static String trim(String src) {
		if (src == null) {
			return null;
		}

		// Replace &nbsp; with space and then trim
		src = src.replace('\u00A0', ' ').trim();
		return src.isEmpty() ? null : src;
	}

	private static String generateCalendar(List<Shift> schedule) throws IOException, ValidationException {
		// Create a TimeZone
		TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
		TimeZone timezone = registry.getTimeZone("America/New_York");
		VTimeZone tz = timezone.getVTimeZone();

		// Create a calendar
		net.fortuna.ical4j.model.Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
		icsCalendar.getProperties().add(new ProdId("-//Ryan Tenney//UofR Schedule Parser//EN"));
		icsCalendar.getProperties().add(Version.VERSION_2_0);
		icsCalendar.getProperties().add(CalScale.GREGORIAN);

		// generate unique identifier..
		UidGenerator ug;
		try {
			ug = new UidGenerator("uidGen");
		}
		catch (SocketException e) {
			throw new RuntimeException("Error creating UidGenerator", e);
		}

		for (Shift shift : schedule) {
			// Create the event
			String eventName = shift.getName();
			net.fortuna.ical4j.model.DateTime start = new net.fortuna.ical4j.model.DateTime(shift.getStartDate().toDate(timezone));
			net.fortuna.ical4j.model.DateTime end = new net.fortuna.ical4j.model.DateTime(shift.getEndDate().toDate(timezone));
			VEvent meeting = new VEvent(start, end, eventName);

			// add timezone and uid
			meeting.getProperties().add(tz.getTimeZoneId());
			meeting.getProperties().add(ug.generateUid());

			// Add the event
			icsCalendar.getComponents().add(meeting);
		}

		StringWriter output = new StringWriter();
		new CalendarOutputter().output(icsCalendar, output);
		return output.toString();
	}

	private static class Shift {

		private final String name;
		private final LocalDateTime startDate;
		private final LocalDateTime endDate;

		public Shift(String name, LocalDateTime startDate, LocalDateTime endDate) {
			this.name = name;
			this.startDate = startDate;
			this.endDate = endDate;
		}

		public String getName() {
			return name;
		}

		public LocalDateTime getStartDate() {
			return startDate;
		}

		public LocalDateTime getEndDate() {
			return endDate;
		}

	}

}
