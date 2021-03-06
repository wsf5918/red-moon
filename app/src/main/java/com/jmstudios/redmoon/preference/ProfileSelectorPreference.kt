/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 *
 *  This file is free software: you may copy, redistribute and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jmstudios.redmoon.preference

import android.app.AlertDialog
import android.content.Context
import android.content.res.TypedArray
import android.preference.Preference
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.helper.Logger
import com.jmstudios.redmoon.model.ProfilesModel
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.util.*

import org.greenrobot.eventbus.Subscribe

private const val DEFAULT_VALUE = 1

class ProfileSelectorPreference(ctx: Context, attrs: AttributeSet) : Preference(ctx, attrs),
                                                                     OnItemSelectedListener {
    lateinit private var mProfileSpinner: Spinner
    lateinit private var mProfileActionButton: Button
    lateinit private var mView: View
    lateinit internal var mArrayAdapter: ArrayAdapter<CharSequence>

    init {
        layoutResource = R.layout.preference_profile_selector
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, DEFAULT_VALUE)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        Config.profile = if (restorePersistedValue) {
            getPersistedInt(DEFAULT_VALUE)
        } else {
            (defaultValue as Int?) ?: DEFAULT_VALUE
        }
    }

    override fun onBindView(view: View) {
        Log.i("onBindView")
        super.onBindView(view)

        mView = view
        mProfileSpinner = view.findViewById(R.id.profile_spinner) as Spinner
        mProfileActionButton = view.findViewById(R.id.profile_action_button) as Button

        initLayout()
        updateButtonSetup()
    }

    private fun initLayout() {
        Log.i("Starting initLayout")
        mArrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item)
        mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        for (i in 0..Config.amountProfiles-1) {
            mArrayAdapter.add(ProfilesModel.getProfileName(i))
        }

        mProfileSpinner.adapter = mArrayAdapter
        mProfileSpinner.setSelection(Config.profile)
        mProfileSpinner.onItemSelectedListener = this
    }

    private fun updateButtonSetup() = if (Config.profile == 0) {
        Log.i("Setting add button")
        mProfileActionButton.text = getString(R.string.button_add_filter)
        mProfileActionButton.setOnClickListener { openAddNewProfileDialog() }
    } else {
        Log.i("Setting remove button")
        mProfileActionButton.text = getString(R.string.button_remove_filter)
        mProfileActionButton.setOnClickListener { openRemoveProfileDialog() }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View,
                                pos: Int, id: Long) {
        Log.i("Item $pos selected")
        persistInt(pos)
    }

    override fun onNothingSelected(parent: AdapterView<*>) { }

    private fun openRemoveProfileDialog() {
        val builder = AlertDialog.Builder(context).apply {
            setTitle(getString(R.string.remove_profile_dialog_title))

            val okString = getString(R.string.button_remove_filter)
            val cancelString = getString(R.string.dialog_button_cancel)

            setNegativeButton(cancelString) { dialog, _ -> dialog.cancel() }
            setPositiveButton(okString) { _, _ ->
                try {
                    Config.amountProfiles = ProfilesModel.removeProfile(Config.profile)
                    persistInt(0)
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("Tried to remove a profile that didn't exist!")
                }
            }
        }
        builder.show()
    }

    private fun openAddNewProfileDialog() {
        val builder = AlertDialog.Builder(context).apply {
            setTitle(getString(R.string.add_new_profile_dialog_title))

            val nameInput = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                hint = getString(R.string.add_new_profile_edit_hint)
            }
            setView(nameInput)

            val okString = getString(R.string.dialog_button_ok)
            val cancelString = getString(R.string.dialog_button_cancel)

            setNegativeButton(cancelString) { dialog, _ -> dialog.cancel() }
            setPositiveButton(okString) { dialog, _ ->
                nameInput.text.toString().run {
                    if (trim { it <= ' ' } != "") {
                        val (size, index) = ProfilesModel.addProfile(this)
                        Config.amountProfiles = size
                        persistInt(index)
                    } else {
                        dialog.cancel()
                    }
                }
            }
        }
        builder.show()
    }

    private fun setCustom() {
        ProfilesModel.setCustom()
        persistInt(0)
    }

    //region presenter
    @Subscribe fun onProfileChanged(event: profileChanged) {
        Log.i("onProfileChanged")
        mProfileSpinner.setSelection(Config.profile)
        updateButtonSetup()
    }

    @Subscribe fun onAmountProfilesChanged(event: amountProfilesChanged) = initLayout()

    @Subscribe fun onDimLevelChanged(event: dimLevelChanged) {
        Log.i("onDimChanged")
        if (Config.dimLevel != activeProfile.dimLevel) { setCustom() }
    }

    @Subscribe fun onIntensityChanged(event: intensityChanged) {
        Log.i("onIntensityChanged")
        if (Config.intensity != activeProfile.intensity) { setCustom() }
    }

    @Subscribe fun onColorChanged(event: colorChanged) {
        Log.i("onColorChanged")
        if (Config.color != activeProfile.color) { setCustom() }
    }

    @Subscribe fun onLowerBrightnessChanged(event: lowerBrightnessChanged) {
        Log.i("onLowerBrightnessChanged")
        if (Config.lowerBrightness != activeProfile.lowerBrightness) { setCustom() }
    }
    //endregion

    companion object : Logger()
}
