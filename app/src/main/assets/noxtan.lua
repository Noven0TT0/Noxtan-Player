noxtan = {}
function noxtan.show_text(text)
    mp.set_property("user-data/noxtan/show_text", text)
end
function noxtan.hide_ui()
    mp.set_property("user-data/noxtan/toggle_ui", "hide")
end
function noxtan.show_ui()
    mp.set_property("user-data/noxtan/toggle_ui", "show")
end
function noxtan.toggle_ui()
    mp.set_property("user-data/noxtan/toggle_ui", "toggle")
end
function noxtan.show_subtitle_settings()
   mp.set_property("user-data/noxtan/show_panel", "subtitle_settings")
end
function noxtan.show_subtitle_delay()
    mp.set_property("user-data/noxtan/show_panel", "subtitle_delay")
end
function noxtan.show_audio_delay()
    mp.set_property("user-data/noxtan/show_panel", "audio_delay")
end
function noxtan.show_video_filters()
    mp.set_property("user-data/noxtan/show_panel", "video_filters")
end
function noxtan.set_button_title(text)
   mp.set_property("user-data/noxtan/set_button_title", text)
end
function noxtan.reset_button_title(text)
    mp.set_property("user-data/noxtan/reset_button_title", "unused")
end
function noxtan.show_button()
    mp.set_property("user-data/noxtan/toggle_button", "show")
end
function noxtan.hide_button()
    mp.set_property("user-data/noxtan/toggle_button", "hide")
end
function noxtan.toggle_button()
    mp.set_property("user-data/noxtan/toggle_button", "toggle")
end
function noxtan.seek_by(value)
    mp.set_property("user-data/noxtan/seek_by", value)
end
function noxtan.seek_to(value)
    mp.set_property("user-data/noxtan/seek_to", value)
end
function noxtan.seek_by_with_text(value, text)
    mp.set_property("user-data/noxtan/seek_by_with_text", value .. "|" .. text)
end
function noxtan.seek_to_with_text(value, text)
    mp.set_property("user-data/noxtan/seek_to_with_text", value .. "|" .. text)
end
function noxtan.show_software_keyboard()
    mp.set_property("user-data/noxtan/software_keyboard", "show")
end
function noxtan.hide_software_keyboard()
    mp.set_property("user-data/noxtan/software_keyboard", "hide")
end
function noxtan.toggle_software_keyboard()
    mp.set_property("user-data/noxtan/software_keyboard", "toggle")
end
return noxtan
