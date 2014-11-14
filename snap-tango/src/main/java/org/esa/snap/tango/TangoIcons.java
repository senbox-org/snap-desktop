package org.esa.snap.tango;

import javax.swing.Icon;

/**
 * @author Norman Fomferra
 */
@SuppressWarnings("UnusedDeclaration")
public final class TangoIcons {

    public enum Res {
        R16("16x16"),
        R22("22x22"),
        R32("32x32");

        private final String name;

        Res(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final static Res R16 = Res.R16;
    public final static Res R22 = Res.R22;
    public final static Res R32 = Res.R32;

    public static Icon actions_address_book_new(Res res) { return getIcon("address-book-new.png", res); }
    public static Icon actions_appointment_new(Res res) { return getIcon("appointment-new.png", res); }
    public static Icon actions_bookmark_new(Res res) { return getIcon("bookmark-new.png", res); }
    public static Icon actions_contact_new(Res res) { return getIcon("contact-new.png", res); }
    public static Icon actions_document_new(Res res) { return getIcon("document-new.png", res); }
    public static Icon actions_document_open(Res res) { return getIcon("document-open.png", res); }
    public static Icon actions_document_print_preview(Res res) { return getIcon("document-print-preview.png", res); }
    public static Icon actions_document_print(Res res) { return getIcon("document-print.png", res); }
    public static Icon actions_document_properties(Res res) { return getIcon("document-properties.png", res); }
    public static Icon actions_document_save_as(Res res) { return getIcon("document-save-as.png", res); }
    public static Icon actions_document_save(Res res) { return getIcon("document-save.png", res); }
    public static Icon actions_edit_clear(Res res) { return getIcon("edit-clear.png", res); }
    public static Icon actions_edit_copy(Res res) { return getIcon("edit-copy.png", res); }
    public static Icon actions_edit_cut(Res res) { return getIcon("edit-cut.png", res); }
    public static Icon actions_edit_delete(Res res) { return getIcon("edit-delete.png", res); }
    public static Icon actions_edit_find_replace(Res res) { return getIcon("edit-find-replace.png", res); }
    public static Icon actions_edit_find(Res res) { return getIcon("edit-find.png", res); }
    public static Icon actions_edit_paste(Res res) { return getIcon("edit-paste.png", res); }
    public static Icon actions_edit_redo(Res res) { return getIcon("edit-redo.png", res); }
    public static Icon actions_edit_select_all(Res res) { return getIcon("edit-select-all.png", res); }
    public static Icon actions_edit_undo(Res res) { return getIcon("edit-undo.png", res); }
    public static Icon actions_folder_new(Res res) { return getIcon("folder-new.png", res); }
    public static Icon actions_format_indent_less(Res res) { return getIcon("format-indent-less.png", res); }
    public static Icon actions_format_indent_more(Res res) { return getIcon("format-indent-more.png", res); }
    public static Icon actions_format_justify_center(Res res) { return getIcon("format-justify-center.png", res); }
    public static Icon actions_format_justify_fill(Res res) { return getIcon("format-justify-fill.png", res); }
    public static Icon actions_format_justify_left(Res res) { return getIcon("format-justify-left.png", res); }
    public static Icon actions_format_justify_right(Res res) { return getIcon("format-justify-right.png", res); }
    public static Icon actions_format_text_bold(Res res) { return getIcon("format-text-bold.png", res); }
    public static Icon actions_format_text_italic(Res res) { return getIcon("format-text-italic.png", res); }
    public static Icon actions_format_text_strikethrough(Res res) { return getIcon("format-text-strikethrough.png", res); }
    public static Icon actions_format_text_underline(Res res) { return getIcon("format-text-underline.png", res); }
    public static Icon actions_go_bottom(Res res) { return getIcon("go-bottom.png", res); }
    public static Icon actions_go_down(Res res) { return getIcon("go-down.png", res); }
    public static Icon actions_go_first(Res res) { return getIcon("go-first.png", res); }
    public static Icon actions_go_home(Res res) { return getIcon("go-home.png", res); }
    public static Icon actions_go_jump(Res res) { return getIcon("go-jump.png", res); }
    public static Icon actions_go_last(Res res) { return getIcon("go-last.png", res); }
    public static Icon actions_go_next(Res res) { return getIcon("go-next.png", res); }
    public static Icon actions_go_previous(Res res) { return getIcon("go-previous.png", res); }
    public static Icon actions_go_top(Res res) { return getIcon("go-top.png", res); }
    public static Icon actions_go_up(Res res) { return getIcon("go-up.png", res); }
    public static Icon actions_list_add(Res res) { return getIcon("list-add.png", res); }
    public static Icon actions_list_remove(Res res) { return getIcon("list-remove.png", res); }
    public static Icon actions_mail_forward(Res res) { return getIcon("mail-forward.png", res); }
    public static Icon actions_mail_mark_junk(Res res) { return getIcon("mail-mark-junk.png", res); }
    public static Icon actions_mail_mark_not_junk(Res res) { return getIcon("mail-mark-not-junk.png", res); }
    public static Icon actions_mail_message_new(Res res) { return getIcon("mail-message-new.png", res); }
    public static Icon actions_mail_reply_all(Res res) { return getIcon("mail-reply-all.png", res); }
    public static Icon actions_mail_reply_sender(Res res) { return getIcon("mail-reply-sender.png", res); }
    public static Icon actions_mail_send_receive(Res res) { return getIcon("mail-send-receive.png", res); }
    public static Icon actions_media_eject(Res res) { return getIcon("media-eject.png", res); }
    public static Icon actions_media_playback_pause(Res res) { return getIcon("media-playback-pause.png", res); }
    public static Icon actions_media_playback_start(Res res) { return getIcon("media-playback-start.png", res); }
    public static Icon actions_media_playback_stop(Res res) { return getIcon("media-playback-stop.png", res); }
    public static Icon actions_media_record(Res res) { return getIcon("media-record.png", res); }
    public static Icon actions_media_seek_backward(Res res) { return getIcon("media-seek-backward.png", res); }
    public static Icon actions_media_seek_forward(Res res) { return getIcon("media-seek-forward.png", res); }
    public static Icon actions_media_skip_backward(Res res) { return getIcon("media-skip-backward.png", res); }
    public static Icon actions_media_skip_forward(Res res) { return getIcon("media-skip-forward.png", res); }
    public static Icon actions_process_stop(Res res) { return getIcon("process-stop.png", res); }
    public static Icon actions_system_lock_screen(Res res) { return getIcon("system-lock-screen.png", res); }
    public static Icon actions_system_log_out(Res res) { return getIcon("system-log-out.png", res); }
    public static Icon actions_system_search(Res res) { return getIcon("system-search.png", res); }
    public static Icon actions_system_shutdown(Res res) { return getIcon("system-shutdown.png", res); }
    public static Icon actions_tab_new(Res res) { return getIcon("tab-new.png", res); }
    public static Icon actions_view_fullscreen(Res res) { return getIcon("view-fullscreen.png", res); }
    public static Icon actions_view_refresh(Res res) { return getIcon("view-refresh.png", res); }
    public static Icon actions_window_new(Res res) { return getIcon("window-new.png", res); }
    public static Icon animations_process_working(Res res) { return getIcon("process-working.png", res); }
    public static Icon apps_accessories_calculator(Res res) { return getIcon("accessories-calculator.png", res); }
    public static Icon apps_accessories_character_map(Res res) { return getIcon("accessories-character-map.png", res); }
    public static Icon apps_accessories_text_editor(Res res) { return getIcon("accessories-text-editor.png", res); }
    public static Icon apps_help_browser(Res res) { return getIcon("help-browser.png", res); }
    public static Icon apps_internet_group_chat(Res res) { return getIcon("internet-group-chat.png", res); }
    public static Icon apps_internet_mail(Res res) { return getIcon("internet-mail.png", res); }
    public static Icon apps_internet_news_reader(Res res) { return getIcon("internet-news-reader.png", res); }
    public static Icon apps_internet_web_browser(Res res) { return getIcon("internet-web-browser.png", res); }
    public static Icon apps_office_calendar(Res res) { return getIcon("office-calendar.png", res); }
    public static Icon apps_preferences_desktop_accessibility(Res res) { return getIcon("preferences-desktop-accessibility.png", res); }
    public static Icon apps_preferences_desktop_assistive_technology(Res res) { return getIcon("preferences-desktop-assistive-technology.png", res); }
    public static Icon apps_preferences_desktop_font(Res res) { return getIcon("preferences-desktop-font.png", res); }
    public static Icon apps_preferences_desktop_keyboard_shortcuts(Res res) { return getIcon("preferences-desktop-keyboard-shortcuts.png", res); }
    public static Icon apps_preferences_desktop_locale(Res res) { return getIcon("preferences-desktop-locale.png", res); }
    public static Icon apps_preferences_desktop_multimedia(Res res) { return getIcon("preferences-desktop-multimedia.png", res); }
    public static Icon apps_preferences_desktop_remote_desktop(Res res) { return getIcon("preferences-desktop-remote-desktop.png", res); }
    public static Icon apps_preferences_desktop_screensaver(Res res) { return getIcon("preferences-desktop-screensaver.png", res); }
    public static Icon apps_preferences_desktop_theme(Res res) { return getIcon("preferences-desktop-theme.png", res); }
    public static Icon apps_preferences_desktop_wallpaper(Res res) { return getIcon("preferences-desktop-wallpaper.png", res); }
    public static Icon apps_preferences_system_network_proxy(Res res) { return getIcon("preferences-system-network-proxy.png", res); }
    public static Icon apps_preferences_system_session(Res res) { return getIcon("preferences-system-session.png", res); }
    public static Icon apps_preferences_system_windows(Res res) { return getIcon("preferences-system-windows.png", res); }
    public static Icon apps_system_file_manager(Res res) { return getIcon("system-file-manager.png", res); }
    public static Icon apps_system_installer(Res res) { return getIcon("system-installer.png", res); }
    public static Icon apps_system_software_update(Res res) { return getIcon("system-software-update.png", res); }
    public static Icon apps_system_users(Res res) { return getIcon("system-users.png", res); }
    public static Icon apps_utilities_system_monitor(Res res) { return getIcon("utilities-system-monitor.png", res); }
    public static Icon apps_utilities_terminal(Res res) { return getIcon("utilities-terminal.png", res); }
    public static Icon categories_applications_accessories(Res res) { return getIcon("applications-accessories.png", res); }
    public static Icon categories_applications_development(Res res) { return getIcon("applications-development.png", res); }
    public static Icon categories_applications_games(Res res) { return getIcon("applications-games.png", res); }
    public static Icon categories_applications_graphics(Res res) { return getIcon("applications-graphics.png", res); }
    public static Icon categories_applications_internet(Res res) { return getIcon("applications-internet.png", res); }
    public static Icon categories_applications_multimedia(Res res) { return getIcon("applications-multimedia.png", res); }
    public static Icon categories_applications_office(Res res) { return getIcon("applications-office.png", res); }
    public static Icon categories_applications_other(Res res) { return getIcon("applications-other.png", res); }
    public static Icon categories_applications_system(Res res) { return getIcon("applications-system.png", res); }
    public static Icon categories_preferences_desktop_peripherals(Res res) { return getIcon("preferences-desktop-peripherals.png", res); }
    public static Icon categories_preferences_desktop(Res res) { return getIcon("preferences-desktop.png", res); }
    public static Icon categories_preferences_system(Res res) { return getIcon("preferences-system.png", res); }
    public static Icon devices_audio_card(Res res) { return getIcon("audio-card.png", res); }
    public static Icon devices_audio_input_microphone(Res res) { return getIcon("audio-input-microphone.png", res); }
    public static Icon devices_battery(Res res) { return getIcon("battery.png", res); }
    public static Icon devices_camera_photo(Res res) { return getIcon("camera-photo.png", res); }
    public static Icon devices_camera_video(Res res) { return getIcon("camera-video.png", res); }
    public static Icon devices_computer(Res res) { return getIcon("computer.png", res); }
    public static Icon devices_drive_harddisk(Res res) { return getIcon("drive-harddisk.png", res); }
    public static Icon devices_drive_optical(Res res) { return getIcon("drive-optical.png", res); }
    public static Icon devices_drive_removable_media(Res res) { return getIcon("drive-removable-media.png", res); }
    public static Icon devices_input_gaming(Res res) { return getIcon("input-gaming.png", res); }
    public static Icon devices_input_keyboard(Res res) { return getIcon("input-keyboard.png", res); }
    public static Icon devices_input_mouse(Res res) { return getIcon("input-mouse.png", res); }
    public static Icon devices_media_flash(Res res) { return getIcon("media-flash.png", res); }
    public static Icon devices_media_floppy(Res res) { return getIcon("media-floppy.png", res); }
    public static Icon devices_media_optical(Res res) { return getIcon("media-optical.png", res); }
    public static Icon devices_multimedia_player(Res res) { return getIcon("multimedia-player.png", res); }
    public static Icon devices_network_wired(Res res) { return getIcon("network-wired.png", res); }
    public static Icon devices_network_wireless(Res res) { return getIcon("network-wireless.png", res); }
    public static Icon devices_printer(Res res) { return getIcon("printer.png", res); }
    public static Icon devices_video_display(Res res) { return getIcon("video-display.png", res); }
    public static Icon emblems_emblem_favorite(Res res) { return getIcon("emblem-favorite.png", res); }
    public static Icon emblems_emblem_important(Res res) { return getIcon("emblem-important.png", res); }
    public static Icon emblems_emblem_photos(Res res) { return getIcon("emblem-photos.png", res); }
    public static Icon emblems_emblem_readonly(Res res) { return getIcon("emblem-readonly.png", res); }
    public static Icon emblems_emblem_symbolic_link(Res res) { return getIcon("emblem-symbolic-link.png", res); }
    public static Icon emblems_emblem_system(Res res) { return getIcon("emblem-system.png", res); }
    public static Icon emblems_emblem_unreadable(Res res) { return getIcon("emblem-unreadable.png", res); }
    public static Icon emotes_face_angel(Res res) { return getIcon("face-angel.png", res); }
    public static Icon emotes_face_crying(Res res) { return getIcon("face-crying.png", res); }
    public static Icon emotes_face_devilish(Res res) { return getIcon("face-devilish.png", res); }
    public static Icon emotes_face_glasses(Res res) { return getIcon("face-glasses.png", res); }
    public static Icon emotes_face_grin(Res res) { return getIcon("face-grin.png", res); }
    public static Icon emotes_face_kiss(Res res) { return getIcon("face-kiss.png", res); }
    public static Icon emotes_face_monkey(Res res) { return getIcon("face-monkey.png", res); }
    public static Icon emotes_face_plain(Res res) { return getIcon("face-plain.png", res); }
    public static Icon emotes_face_sad(Res res) { return getIcon("face-sad.png", res); }
    public static Icon emotes_face_smile_big(Res res) { return getIcon("face-smile-big.png", res); }
    public static Icon emotes_face_smile(Res res) { return getIcon("face-smile.png", res); }
    public static Icon emotes_face_surprise(Res res) { return getIcon("face-surprise.png", res); }
    public static Icon emotes_face_wink(Res res) { return getIcon("face-wink.png", res); }
    public static Icon mimetypes_application_certificate(Res res) { return getIcon("application-certificate.png", res); }
    public static Icon mimetypes_application_x_executable(Res res) { return getIcon("application-x-executable.png", res); }
    public static Icon mimetypes_audio_x_generic(Res res) { return getIcon("audio-x-generic.png", res); }
    public static Icon mimetypes_font_x_generic(Res res) { return getIcon("font-x-generic.png", res); }
    public static Icon mimetypes_image_x_generic(Res res) { return getIcon("image-x-generic.png", res); }
    public static Icon mimetypes_package_x_generic(Res res) { return getIcon("package-x-generic.png", res); }
    public static Icon mimetypes_text_html(Res res) { return getIcon("text-html.png", res); }
    public static Icon mimetypes_text_x_generic_template(Res res) { return getIcon("text-x-generic-template.png", res); }
    public static Icon mimetypes_text_x_generic(Res res) { return getIcon("text-x-generic.png", res); }
    public static Icon mimetypes_text_x_script(Res res) { return getIcon("text-x-script.png", res); }
    public static Icon mimetypes_video_x_generic(Res res) { return getIcon("video-x-generic.png", res); }
    public static Icon mimetypes_x_office_address_book(Res res) { return getIcon("x-office-address-book.png", res); }
    public static Icon mimetypes_x_office_calendar(Res res) { return getIcon("x-office-calendar.png", res); }
    public static Icon mimetypes_x_office_document_template(Res res) { return getIcon("x-office-document-template.png", res); }
    public static Icon mimetypes_x_office_document(Res res) { return getIcon("x-office-document.png", res); }
    public static Icon mimetypes_x_office_drawing_template(Res res) { return getIcon("x-office-drawing-template.png", res); }
    public static Icon mimetypes_x_office_drawing(Res res) { return getIcon("x-office-drawing.png", res); }
    public static Icon mimetypes_x_office_presentation_template(Res res) { return getIcon("x-office-presentation-template.png", res); }
    public static Icon mimetypes_x_office_presentation(Res res) { return getIcon("x-office-presentation.png", res); }
    public static Icon mimetypes_x_office_spreadsheet_template(Res res) { return getIcon("x-office-spreadsheet-template.png", res); }
    public static Icon mimetypes_x_office_spreadsheet(Res res) { return getIcon("x-office-spreadsheet.png", res); }
    public static Icon places_folder_remote(Res res) { return getIcon("folder-remote.png", res); }
    public static Icon places_folder_saved_search(Res res) { return getIcon("folder-saved-search.png", res); }
    public static Icon places_folder(Res res) { return getIcon("folder.png", res); }
    public static Icon places_network_server(Res res) { return getIcon("network-server.png", res); }
    public static Icon places_network_workgroup(Res res) { return getIcon("network-workgroup.png", res); }
    public static Icon places_start_here(Res res) { return getIcon("start-here.png", res); }
    public static Icon places_user_desktop(Res res) { return getIcon("user-desktop.png", res); }
    public static Icon places_user_home(Res res) { return getIcon("user-home.png", res); }
    public static Icon places_user_trash(Res res) { return getIcon("user-trash.png", res); }
    public static Icon status_audio_volume_high(Res res) { return getIcon("audio-volume-high.png", res); }
    public static Icon status_audio_volume_low(Res res) { return getIcon("audio-volume-low.png", res); }
    public static Icon status_audio_volume_medium(Res res) { return getIcon("audio-volume-medium.png", res); }
    public static Icon status_audio_volume_muted(Res res) { return getIcon("audio-volume-muted.png", res); }
    public static Icon status_battery_caution(Res res) { return getIcon("battery-caution.png", res); }
    public static Icon status_dialog_error(Res res) { return getIcon("dialog-error.png", res); }
    public static Icon status_dialog_information(Res res) { return getIcon("dialog-information.png", res); }
    public static Icon status_dialog_warning(Res res) { return getIcon("dialog-warning.png", res); }
    public static Icon status_folder_drag_accept(Res res) { return getIcon("folder-drag-accept.png", res); }
    public static Icon status_folder_open(Res res) { return getIcon("folder-open.png", res); }
    public static Icon status_folder_visiting(Res res) { return getIcon("folder-visiting.png", res); }
    public static Icon status_image_loading(Res res) { return getIcon("image-loading.png", res); }
    public static Icon status_image_missing(Res res) { return getIcon("image-missing.png", res); }
    public static Icon status_mail_attachment(Res res) { return getIcon("mail-attachment.png", res); }
    public static Icon status_network_error(Res res) { return getIcon("network-error.png", res); }
    public static Icon status_network_idle(Res res) { return getIcon("network-idle.png", res); }
    public static Icon status_network_offline(Res res) { return getIcon("network-offline.png", res); }
    public static Icon status_network_receive(Res res) { return getIcon("network-receive.png", res); }
    public static Icon status_network_transmit_receive(Res res) { return getIcon("network-transmit-receive.png", res); }
    public static Icon status_network_transmit(Res res) { return getIcon("network-transmit.png", res); }
    public static Icon status_network_wireless_encrypted(Res res) { return getIcon("network-wireless-encrypted.png", res); }
    public static Icon status_printer_error(Res res) { return getIcon("printer-error.png", res); }
    public static Icon status_software_update_available(Res res) { return getIcon("software-update-available.png", res); }
    public static Icon status_software_update_urgent(Res res) { return getIcon("software-update-urgent.png", res); }
    public static Icon status_user_trash_full(Res res) { return getIcon("user-trash-full.png", res); }
    public static Icon status_weather_clear_night(Res res) { return getIcon("weather-clear-night.png", res); }
    public static Icon status_weather_clear(Res res) { return getIcon("weather-clear.png", res); }
    public static Icon status_weather_few_clouds_night(Res res) { return getIcon("weather-few-clouds-night.png", res); }
    public static Icon status_weather_few_clouds(Res res) { return getIcon("weather-few-clouds.png", res); }
    public static Icon status_weather_overcast(Res res) { return getIcon("weather-overcast.png", res); }
    public static Icon status_weather_severe_alert(Res res) { return getIcon("weather-severe-alert.png", res); }
    public static Icon status_weather_showers_scattered(Res res) { return getIcon("weather-showers-scattered.png", res); }
    public static Icon status_weather_showers(Res res) { return getIcon("weather-showers.png", res); }
    public static Icon status_weather_snow(Res res) { return getIcon("weather-snow.png", res); }
    public static Icon status_weather_storm(Res res) { return getIcon("weather-storm.png", res); }


    private TangoIcons() {}

    private static Icon getIcon(String name, Res res) {
        return null;
    }
}