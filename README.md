# 🌟 Freshcart. Android Food Rescue Application: Fighting Waste, Feeding Hope 🍽️


![Android-Studio-green](https://github.com/user-attachments/assets/f2f41d10-69ba-4d87-bc73-7731cff6ac90)

![Firebase-Auth   Firestore-orange](https://github.com/user-attachments/assets/8c5542f7-6c50-4235-8fe2-257a56e9e607)

![Made with-Love-ff69b4](https://github.com/user-attachments/assets/80642a20-7066-4ffc-b30b-1d143b92fc30)

## 📱 Application Overview
FoodResQ is an Android application built to connect food establishments with consumers, offering near-expiry food at reduced prices. The app implements modern Android development practices with Kotlin and Firebase integration.

## 🔑 Authentication Features
- Multiple Sign-in Options:
  
      // Google Authentication Implementation
      val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
          .requestIdToken(getString(R.string.default_web_client_id))
          .requestEmail()
          .build()
- Email/Password Registration
- Google Sign-in
- Session Management

## 🏗️ Core Components
- Home Screen

      class Home : Activity() {
          private val filteredList = mutableListOf<Product>()
          private lateinit var foodList: RecyclerView
          private lateinit var searchResultsList: RecyclerView
          
          // Dynamic Product Loading
          private fun filterProducts(query: String) {
              filteredList.clear()
              // Search implementation
          }
      }
  
- Product Display
  
      class ProductAdapter(
          private var foods: List<Product>, 
          private val context: Context
      ) : RecyclerView.Adapter<ProductAdapter.MyViewHolder>() {
          // Custom adapter for product display
      }
