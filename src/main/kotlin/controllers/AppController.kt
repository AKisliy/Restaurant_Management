package controllers

import FilesController
import InputController
import infrastructure.ObservableList
import OutputController
import enums.FilePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import models.*
import repository.AdminRepository
import repository.MenuRepository
import repository.OrderRepository
import repository.UserRepository
import java.lang.Thread.sleep

/**
 * AppController - class which is responsible for main app's stages: start, processing and finish
 */
class AppController {
    private val filesController = FilesController()
    private lateinit var userController: UserController
    private lateinit var adminController: AdminController
    private lateinit var loginController: LoginController
    private val outputController: OutputController = OutputController()
    private val inputController: InputController = InputController(::printError)
    private lateinit var orderController: OrderController

    init{
        launchApp()
    }

    /**
     * launch - starts the app. Take all the information from JSON files and returns CinemaManager with proper lists
     */
    private fun launchApp(){
        val users = filesController.getData<MutableList<User>>(FilePaths.USERS_FILE.path)
        val admins = filesController.getData<MutableList<Admin>>(FilePaths.ADMINS_FILE.path)
        val menu = filesController.getData<MutableList<MenuItem>>(FilePaths.MENU_FILE.path)
        val orders = filesController.getData<MutableList<Order>>(FilePaths.ORDERS_FILE.path)
        val restaurant = filesController.getData<Restaurant>(FilePaths.RESTAURANT_FILE.path)

        val oUsers = ObservableList(users)
        val oAdmins = ObservableList(admins)
        val oMenu = ObservableList(menu)
        val oOrders = ObservableList(orders)

        oUsers.addObserver { _, _ -> filesController.saveChanges(users, FilePaths.USERS_FILE.path) }
        oAdmins.addObserver{ _, _ -> filesController.saveChanges(admins, FilePaths.ADMINS_FILE.path)}
        oMenu.addObserver{_, _ -> filesController.saveChanges(menu, FilePaths.MENU_FILE.path)}
        oOrders.addObserver{_,_ -> filesController.saveChanges(orders, FilePaths.ORDERS_FILE.path)}

        val usersRepository = UserRepository(oUsers)
        val adminRepository = AdminRepository(oAdmins)
        val menuRepository = MenuRepository(oMenu)
        val orderRepository = OrderRepository(oOrders)

        loginController = LoginController(outputController, inputController, usersRepository, adminRepository)
        adminController = AdminController(outputController, inputController, menuRepository)
        orderController = OrderController(restaurant, menuRepository)
        userController = UserController(menuRepository,orderController, orderRepository, null)
        orderController.processOrders()
    }

    /**
     * appProcess - processing the app
     */
    fun processApp(){
        while(true) {
            outputController.printMessage("Welcome to Restaurant management system!!")
            var user = loginController.getUser()
            while (user == null) {
                outputController.printMessage("Ooops, seems like something went wrong during authorization.")
                outputController.printMessage("Quit app?(Y/N)")
                if (inputController.getUserApproval())
                    return
                user = loginController.getUser()
            }
            outputController.printMessage("Now you're in system!")
            outputController.printMessage("Starting orders' screen...")
            orderController.startRestaurantBoard()
            if (user is Admin) {
                processAdminScenario(user)
            } else {
                processUserScenario(user as User)
            }
        }
    }

    /**
     * processAdminScenario - app scenario if admin registered
     */
    private fun processAdminScenario(admin: Admin?){
        outputController.printMessage("You're in admin panel.")
        while(true) {
            outputController.printMessage("Choose option:")
            outputController.printNumberedList(adminController.adminFunctions)
            val choice = inputController.getNumberInRange(1, adminController.functionsNumber)
            adminController.processFunction(choice)
            outputController.printMessage("Quit admin panel?(Y/N)")
            if(inputController.getUserApproval())
                break
        }
    }

    /**
     * processUserScenario - app scenario if user registered
     */
    private fun processUserScenario(user: User?){
        val newUserController = UserController(userController)
        newUserController.setUser(user!!)
        outputController.printMessage("Choose option")
        outputController.printMessage(newUserController.getFunctionsString())
        inputController.getNumberInRange(1, newUserController.functionsNumber)
        // start new coroutine to serve this client
        CoroutineScope(Dispatchers.Default).launch {
            newUserController.serveClient()
        }
    }

    /**
     * printError - method for inputController to notify about errors
     */
    private fun printError(message: String){
        outputController.printMessage(message)
    }
}