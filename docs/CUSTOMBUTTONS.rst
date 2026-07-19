CUSTOM BUTTONS
==============

Custom buttons provides a way to execute lua code by pressing a button in the player. NoxtanPlayer also provides an interface to interact with some parts of the player.

The interface is defined in a file placed in the ``scripts`` directory and can be accessed through the ``NoxtanPlayer`` table.

Lua interface
-------------

``NoxtanPlayer.show_text(text)``
    Display a message on the player.

``NoxtanPlayer.hide_ui()``
    Hide the player UI.

``NoxtanPlayer.show_ui()``
    Show the player UI.

``NoxtanPlayer.toggle_ui()``
    Toggle the visibility of the player UI.

``NoxtanPlayer.show_subtitle_settings()``
    Show the subtitle settings sheet.

``NoxtanPlayer.show_subtitle_delay()``
    Show the subtitle delay sheet.

``NoxtanPlayer.show_audio_delay()``
    Show the subtitle delay sheet.

``NoxtanPlayer.show_video_filters()``
    Show the video filters sheet.

``NoxtanPlayer.set_button_title(text)``
    Change the title for the primary custom button.

``NoxtanPlayer.reset_button_title(text)``
    Reset the title for the primary custom button.

``NoxtanPlayer.show_button()``
    Show the primary custom button.

``NoxtanPlayer.hide_button()``
    Hide the primary custom button.

``NoxtanPlayer.toggle_button()``
    Toggle the visibility of the primary custom button.

``NoxtanPlayer.seek_by(value)``
    Seek by the specified number of seconds.

``NoxtanPlayer.seek_to(value)``
    Seek to the specified position in seconds.

``NoxtanPlayer.seek_by_with_text(value, text)``
    Seek by the specified number of seconds and display the given text.

``NoxtanPlayer.seek_to_with_text(value, text)``
    Seek to the specified position in seconds and display the given text.

``NoxtanPlayer.hide_software_keyboard()``
    Hide the software keyboard.

``NoxtanPlayer.show_software_keyboard()``
    Show the software keyboard.

``NoxtanPlayer.toggle_software_keyboard()``
    Toggle the visibility of the software keyboard.

Call a custom button from key input or from lua
-----------------------------------------------

Custom buttons can be called from key inputs or from other lua scripts, if so desired. This is done through ``script-message`` with the message ``call_button_<id>`` for normal press and ``call_button_<id>_long`` for long press, where ``<id>`` is the id for the button (shown in top right when editing a button).

Example: ``a script-message call_button_1`` will call the button of id 1 when ``a`` is pressed, if added to ``input.conf``.
