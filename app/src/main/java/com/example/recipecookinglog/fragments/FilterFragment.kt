package com.example.recipecookinglog.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.recipecookinglog.databinding.FragmentFilterBinding
import com.example.recipecookinglog.utils.Constants
import com.example.recipecookinglog.viewmodels.RecipeViewModel

class FilterFragment : DialogFragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private val recipeViewModel: RecipeViewModel by activityViewModels()

    private var selectedCuisine: String = "All"
    private var selectedMealType: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupClickListeners()
    }

    private fun setupSpinners() {
        // Cuisine spinner
        val cuisineAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Constants.CUISINES
        )
        cuisineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCuisine.adapter = cuisineAdapter

        // Meal type spinner
        val mealTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Constants.MEAL_TYPES
        )
        mealTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMealType.adapter = mealTypeAdapter
    }

    private fun setupClickListeners() {
        binding.btnApply.setOnClickListener {
            selectedCuisine = binding.spinnerCuisine.selectedItem.toString()
            selectedMealType = binding.spinnerMealType.selectedItem.toString()

            val cuisine = if (selectedCuisine == "All") null else selectedCuisine
            val mealType = if (selectedMealType == "All") null else selectedMealType

            recipeViewModel.syncRecipes(cuisine, mealType)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnReset.setOnClickListener {
            binding.spinnerCuisine.setSelection(0)
            binding.spinnerMealType.setSelection(0)
            recipeViewModel.syncRecipes()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
