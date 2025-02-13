# 🌟 Freshcart. Android Food Rescue Application: Fighting Waste, Feeding Hope 🍽️


![Android-Studio-green](https://github.com/user-attachments/assets/f2f41d10-69ba-4d87-bc73-7731cff6ac90) ![Firebase-Auth   Firestore-orange](https://github.com/user-attachments/assets/8c5542f7-6c50-4235-8fe2-257a56e9e607) ![Made with-Love-ff69b4](https://github.com/user-attachments/assets/80642a20-7066-4ffc-b30b-1d143b92fc30)

> ## "If food waste were a country, its greenhouse gas emissions would be the third largest in the world, following the US and China." 🌍

## 🎯 Our Mission
Transforming food waste into opportunities while making quality food accessible to everyone. We're building a revolutionary ecosystem that connects food establishments with consumers, offering near-expiry food at significantly reduced prices.

## 📊 Impact Statistics
### Food Waste Reduction 🗑️
- Currently, 40% of food is wasted annually
- 1200k tons wasted in our region
- 78% of wasted food is still edible
### Social Impact 💝
- Targeting 5.1% population below poverty line
- Supporting 750k people under the breadline
- Making quality food accessible to all

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
## 🔥 Firebase Integration
- Authentication
  - User registration and login
  - Profile management
  - Session persistence
- Firestore Database

        val fireDb = Firebase.firestore
        val usersCol = fireDb.collection("users")
  
  - Product data storage
  - User cart management
  - Order history
