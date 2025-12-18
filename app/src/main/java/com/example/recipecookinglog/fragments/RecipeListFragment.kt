package com.example.recipecookinglog.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipecookinglog.activities.RecipeDetailActivity
import com.example.recipecookinglog.adapters.RecipeAdapter
import com.example.recipecookinglog.databinding.FragmentRecipeListBinding
import com.example.recipecookinglog.models.Recipe
import com.example.recipecookinglog.utils.Constants
import com.example.recipecookinglog.viewmodels.RecipeViewModel

class RecipeListFragment : Fragment() {

    private var _binding: FragmentRecipeListBinding? = null
    private val binding get() = _binding!!

    private val recipeViewModel: RecipeViewModel by activityViewModels()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()

        // Initial sync
        recipeViewModel.syncRecipes()
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter { recipe ->
            openRecipeDetail(recipe)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recipeAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        recipeViewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            if (recipes.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                recipeAdapter.submitList(recipes)
            }
        }

        recipeViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

        recipeViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                recipeViewModel.clearMessages()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            recipeViewModel.syncRecipes()
        }
    }

    private fun openRecipeDetail(recipe: Recipe) {
        val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
        intent.putExtra(Constants.EXTRA_RECIPE_ID, recipe.id)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh when coming back from detail/edit
        recipeViewModel.syncRecipes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}