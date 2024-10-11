package com.example.privatevpn

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.util.Locale

class BottomsheetFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private var isShowing: Boolean = false // Track if BottomSheet is showing

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottomsheet_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("app_language", Context.MODE_PRIVATE)

        // Load and set the current selected language
        loadSelectedLanguage(view)

        // Set up click listeners for each language TextView and CheckBox
        setupLanguageClickListeners(view)
    }

    private fun setupLanguageClickListeners(view: View) {
        // Find TextViews and CheckBoxes
        val textEnglish: TextView = view.findViewById(R.id.textEnglish)
        val textAfrikaans: TextView = view.findViewById(R.id.textAfrikaans)
        val textArabic: TextView = view.findViewById(R.id.textArabic)
        val textChinese: TextView = view.findViewById(R.id.textChinese)
        val textCzech: TextView = view.findViewById(R.id.textCzech)
        val textDanish: TextView = view.findViewById(R.id.textDanish)
        val textDutch: TextView = view.findViewById(R.id.textDutch)
        val textFrench: TextView = view.findViewById(R.id.textFrench)
        val textGerman: TextView = view.findViewById(R.id.textGerman)
        val textGreek: TextView = view.findViewById(R.id.textGreek)
        val textHindi: TextView = view.findViewById(R.id.textHindi)
        val textIndonesian: TextView = view.findViewById(R.id.textIndonesian)
        val textItalian: TextView = view.findViewById(R.id.textItalian)
        val textJapanese: TextView = view.findViewById(R.id.textJapanese)
        val textMalay: TextView = view.findViewById(R.id.textMalay)
        val textKorean: TextView = view.findViewById(R.id.textKorean)
        val textNorwegian: TextView = view.findViewById(R.id.textNorwegian)
        val textPersian: TextView = view.findViewById(R.id.textPersian)
        val textPortuguese: TextView = view.findViewById(R.id.textPortuguese)
        val textRussian: TextView = view.findViewById(R.id.textRussian)
        val textSpanish: TextView = view.findViewById(R.id.textSpanish)
        val textThai: TextView = view.findViewById(R.id.textThai)
        val textTurkish: TextView = view.findViewById(R.id.textTurkish)
        val textVietnamese: TextView = view.findViewById(R.id.textVietnamese)

        val checkBoxEnglish: CheckBox = view.findViewById(R.id.checkboxEnglish)
        val checkBoxAfrikaans: CheckBox = view.findViewById(R.id.checkboxAfrikaans)
        val checkBoxArabic: CheckBox = view.findViewById(R.id.checkboxArabic)
        val checkBoxChinese: CheckBox = view.findViewById(R.id.checkboxChinese)
        val checkBoxCzech: CheckBox = view.findViewById(R.id.checkboxCzech)
        val checkBoxDanish: CheckBox = view.findViewById(R.id.checkboxDanish)
        val checkBoxDutch: CheckBox = view.findViewById(R.id.checkboxDutch)
        val checkBoxFrench: CheckBox = view.findViewById(R.id.checkboxFrench)
        val checkBoxGerman: CheckBox = view.findViewById(R.id.checkboxGerman)
        val checkBoxGreek: CheckBox = view.findViewById(R.id.checkboxGreek)
        val checkBoxHindi: CheckBox = view.findViewById(R.id.checkboxHindi)
        val checkBoxIndonesian: CheckBox = view.findViewById(R.id.checkboxIndonesian)
        val checkBoxItalian: CheckBox = view.findViewById(R.id.checkboxItalian)
        val checkBoxJapanese: CheckBox = view.findViewById(R.id.checkboxJapanese)
        val checkBoxMalay: CheckBox = view.findViewById(R.id.checkboxMalay)
        val checkBoxKorean: CheckBox = view.findViewById(R.id.checkboxKorean)
        val checkBoxNorwegian: CheckBox = view.findViewById(R.id.checkboxNorwegian)
        val checkBoxPersian: CheckBox = view.findViewById(R.id.checkboxPersian)
        val checkBoxPortuguese: CheckBox = view.findViewById(R.id.checkboxPortuguese)
        val checkBoxRussian: CheckBox = view.findViewById(R.id.checkboxRussian)
        val checkBoxSpanish: CheckBox = view.findViewById(R.id.checkboxSpanish)
        val checkBoxThai: CheckBox = view.findViewById(R.id.checkboxThai)
        val checkBoxTurkish: CheckBox = view.findViewById(R.id.checkboxTurkish)
        val checkBoxVietnamese: CheckBox = view.findViewById(R.id.checkboxVietnamese)

        // Set click listeners for TextViews and CheckBoxes (linked together)
        setClickListener(textEnglish, checkBoxEnglish, "en", "English")
        setClickListener(textAfrikaans, checkBoxAfrikaans, "af", "Afrikaans")
        setClickListener(textArabic, checkBoxArabic, "ar", "Arabic")
        setClickListener(textChinese, checkBoxChinese, "zh", "Chinese")
        setClickListener(textCzech, checkBoxCzech, "cs", "Czech")
        setClickListener(textDanish, checkBoxDanish, "da", "Danish")
        setClickListener(textDutch, checkBoxDutch, "nl", "Dutch")
        setClickListener(textFrench, checkBoxFrench, "fr", "French")
        setClickListener(textGerman, checkBoxGerman, "de", "German")
        setClickListener(textGreek, checkBoxGreek, "el", "Greek")
        setClickListener(textHindi, checkBoxHindi, "hi", "Hindi")
        setClickListener(textIndonesian, checkBoxIndonesian, "in", "Indonesian")
        setClickListener(textItalian, checkBoxItalian, "it", "Italian")
        setClickListener(textJapanese, checkBoxJapanese, "ja", "Japanese")
        setClickListener(textMalay, checkBoxMalay, "ms", "Malay")
        setClickListener(textKorean, checkBoxKorean, "ko", "Korean")
        setClickListener(textNorwegian, checkBoxNorwegian, "no", "Norwegian")
        setClickListener(textPersian, checkBoxPersian, "fa", "Persian")
        setClickListener(textPortuguese, checkBoxPortuguese, "pt", "Portuguese")
        setClickListener(textRussian, checkBoxRussian, "ru", "Russian")
        setClickListener(textSpanish, checkBoxSpanish, "es", "Spanish")
        setClickListener(textThai, checkBoxThai, "th", "Thai")
        setClickListener(textTurkish, checkBoxTurkish, "tr", "Turkish")
        setClickListener(textVietnamese, checkBoxVietnamese, "vi", "Vietnamese")
    }

    // Set click listeners for both TextView and CheckBox to change the language
    private fun setClickListener(textView: TextView, checkBox: CheckBox, languageCode: String, languageName: String) {
        val clickListener = View.OnClickListener {
            changeLanguage(languageCode, languageName)
            checkBox.isChecked = true // Update the checkbox state
        }
        textView.setOnClickListener(clickListener)
        checkBox.setOnClickListener(clickListener)
    }

    private fun loadSelectedLanguage(view: View) {
        // Load the selected language from SharedPreferences
        val selectedLanguage = sharedPreferences.getString("selected_language", "en") // Default to English

        // Automatically check the selected language's CheckBox
        when (selectedLanguage) {
            "en" -> view.findViewById<CheckBox>(R.id.checkboxEnglish).isChecked = true
            "af" -> view.findViewById<CheckBox>(R.id.checkboxAfrikaans).isChecked = true
            "ar" -> view.findViewById<CheckBox>(R.id.checkboxArabic).isChecked = true
            "zh" -> view.findViewById<CheckBox>(R.id.checkboxChinese).isChecked = true
            "cs" -> view.findViewById<CheckBox>(R.id.checkboxCzech).isChecked = true
            "da" -> view.findViewById<CheckBox>(R.id.checkboxDanish).isChecked = true
            "nl" -> view.findViewById<CheckBox>(R.id.checkboxDutch).isChecked = true
            "fr" -> view.findViewById<CheckBox>(R.id.checkboxFrench).isChecked = true
            "de" -> view.findViewById<CheckBox>(R.id.checkboxGerman).isChecked = true
            "el" -> view.findViewById<CheckBox>(R.id.checkboxGreek).isChecked = true
            "hi" -> view.findViewById<CheckBox>(R.id.checkboxHindi).isChecked = true
            "in" -> view.findViewById<CheckBox>(R.id.checkboxIndonesian).isChecked = true
            "it" -> view.findViewById<CheckBox>(R.id.checkboxItalian).isChecked = true
            "ja" -> view.findViewById<CheckBox>(R.id.checkboxJapanese).isChecked = true
            "ms" -> view.findViewById<CheckBox>(R.id.checkboxMalay).isChecked = true
            "ko" -> view.findViewById<CheckBox>(R.id.checkboxKorean).isChecked = true
            "no" -> view.findViewById<CheckBox>(R.id.checkboxNorwegian).isChecked = true
            "fa" -> view.findViewById<CheckBox>(R.id.checkboxPersian).isChecked = true
            "pt" -> view.findViewById<CheckBox>(R.id.checkboxPortuguese).isChecked = true
            "ru" -> view.findViewById<CheckBox>(R.id.checkboxRussian).isChecked = true
            "es" -> view.findViewById<CheckBox>(R.id.checkboxSpanish).isChecked = true
            "th" -> view.findViewById<CheckBox>(R.id.checkboxThai).isChecked = true
            "tr" -> view.findViewById<CheckBox>(R.id.checkboxTurkish).isChecked = true
            "vi" -> view.findViewById<CheckBox>(R.id.checkboxVietnamese).isChecked = true
        }

        // Set the locale based on the selected language
        setLocale(selectedLanguage ?: "en")
    }

    private fun changeLanguage(languageCode: String, languageName: String) {
        if (isShowing) return // Prevent multiple clicks

        isShowing = true // Set to true when the language is changing

        // Save the selected language in SharedPreferences
        sharedPreferences.edit().putString("selected_language", languageCode).apply()

        // Set the locale for the app
        setLocale(languageCode)

        // Show a toast message
        Toast.makeText(requireContext(), "Language changed to $languageName", Toast.LENGTH_SHORT).show()

        // Notify the activity to recreate itself to apply the new language
        requireActivity().recreate()

        // Close the BottomSheetFragment
        requireActivity().onBackPressedDispatcher.onBackPressed()

        // Reset isShowing to false after a brief delay
        view?.postDelayed({ isShowing = false }, 300) // Delay for 300ms
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = requireContext().resources.configuration
        config.setLocale(locale)
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)
    }
}
