//AdminController
package com.example.E_Comm.Controller;

import com.example.E_Comm.Util.CommonUtil;
import com.example.E_Comm.Util.OrderStatus;
import com.example.E_Comm.model.Category;
import com.example.E_Comm.model.Product;
import com.example.E_Comm.model.ProductOrder;
import com.example.E_Comm.model.UserDetails;
import com.example.E_Comm.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.print.attribute.standard.MediaSize;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @GetMapping("/")
    public String index(){
        return "admin/index";
    }





//-----------------------CATEGORY------------------------

    @GetMapping("/Category")
    public String Category(Model m, @RequestParam(name = "pageNo", defaultValue = "0")Integer pageNo,
                           @RequestParam(name = "pageSize",defaultValue = "3")Integer pageSize) {
        //m.addAttribute("categorys", categoryService.getAllCategory());

        Page<Category> page = categoryService.getAllCategoryPagination(pageNo, pageSize);
        List<Category> categorys = page.getContent();

        // Add pagination and product details to the model
        m.addAttribute("categorys", categorys);
        m.addAttribute("pageNo", page.getNumber());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "admin/add_category";
    }


    @PostMapping("/saveCategory")
    public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
                               HttpSession session) throws IOException {

        // Log isActive for debugging
        System.out.println("isActive from form: " + category.isActive());

        // Set default image name if no file is uploaded
        String imageName = (file != null && !file.isEmpty()) ? file.getOriginalFilename() : "default.jpg";
        category.setImageName(imageName);

        // Check if the category already exists
        Boolean existsCategory = categoryService.existCategory(category.getName());
        if (existsCategory) {
            session.setAttribute("Error", "Category Name already exists");
        } else {
            // Save the new category if it does not exist
            Category savedCategory = categoryService.saveCategory(category);
            if (ObjectUtils.isEmpty(savedCategory)) {
                session.setAttribute("Error", "Not Saved! Internal Server Error");
            } else {

                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "Category" + File.separator + file.getOriginalFilename());
                System.out.println(path);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                session.setAttribute("Success", "Saved Successfully");
            }
        }
        return "redirect:/admin/Category";
    }


    @GetMapping("/deleteCategory/{id}")
    public String deleteCategory(@PathVariable int id, HttpSession session){

        Boolean deleteCategory =  categoryService.deleteCategory(id);
        if (deleteCategory){
            session.setAttribute("Success","category deleted successfully");
        }else {
            session.setAttribute("Error","Something wrong on server");
        }
        return "redirect:/admin/Category";
    }

    @GetMapping("/EditCategory/{id}")
    public String EditCategory(@PathVariable int id, Model m) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            throw new IllegalArgumentException("Category with ID " + id + " not found");
        }
        m.addAttribute("category", category);
        return "/admin/Edit_Category";
    }


    @PostMapping("/updateCategory")
    public String updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file, HttpSession session) throws IOException {
        // Fetch the existing category
        Category oldCategory = categoryService.getCategoryById(category.getId());

        if (oldCategory != null) {
            System.out.println("Old Category: " + oldCategory);
            System.out.println("Updated isActive: " + category.isActive());

            // Update fields
            oldCategory.setName(category.getName());
            oldCategory.setActive(category.isActive()); // Ensure this updates correctly

            // Handle file upload
            String imageName = file.isEmpty() ? oldCategory.getImageName() : file.getOriginalFilename();
            oldCategory.setImageName(imageName);

            // Save updated category
            Category updatedCategory = categoryService.saveCategory(oldCategory);

            if (updatedCategory != null) {
                if (!file.isEmpty()) {
                    // Save the file to the file system
                    File saveFile = new ClassPathResource("static/img").getFile();
                    Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "Category" + File.separator + file.getOriginalFilename());
                    Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                }
                session.setAttribute("Success", "Category updated successfully");
            } else {
                session.setAttribute("Error", "Failed to update category");
            }
        } else {
            session.setAttribute("Error", "Category not found");
        }

        return "redirect:/admin/Category";
    }



//---------------PRODUCTS------------------------------------

    @GetMapping("/addProduct")
    public String addProduct(Model m){
        List<Category> categories =  categoryService.getAllCategory();
        m.addAttribute("categories",categories);
        return "admin/add_product";
    }

    @PostMapping("/saveProduct")
    public String saveProduct(
            @ModelAttribute Product product,
            @RequestParam("file") MultipartFile image,
            @RequestParam("category") int categoryId, // Category ID from the form
            HttpSession session
    ) throws IOException {

        // Handle image upload
        String imageName = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();
        product.setImage(imageName);
        product.setDiscount(0);
        product.setDiscountPrice(product.getPrice());

        // Fetch the Category object using the category ID
        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            session.setAttribute("Error", "Invalid Category Selected");
            return "redirect:/admin/addProduct";
        }

        product.setCategory(category);

        // Save the product
        Product savedProduct = productService.saveProduct(product);
        if (savedProduct != null) {
            // Save the image to the file system
            File saveFile = new ClassPathResource("static/img").getFile();
            Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "Products" + File.separator + image.getOriginalFilename());
            Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            session.setAttribute("Success", "Product Saved Successfully");
        } else {
            session.setAttribute("Error", "Something went wrong!");
        }

        return "redirect:/admin/addProduct";
    }






//-----------------------VIEW PRODUCTS-----------------------------------------

    @GetMapping("/viewProducts")
    public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch , @RequestParam(name = "pageNo", defaultValue = "0")Integer pageNo,
                                  @RequestParam(name = "pageSize",defaultValue = "3")Integer pageSize){



//        if (ch!=null && ch.length() > 0){
//            products = productService.searchProduct(ch);
//        }else {
//            products = productService.getAllProduct();
//        }
//        products.forEach(product -> System.out.println("Product: " + product));
//        m.addAttribute("products", products);


        Page<Product> page;

        if (ch != null && !ch.isEmpty()) {
            page = productService.searchProductPagination(pageNo, pageSize, ch);
        } else {
            page = productService.getAllProductsPagination(pageNo, pageSize);
        }

        m.addAttribute("products", page.getContent());
        m.addAttribute("pageNo", page.getNumber());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        m.addAttribute("paramValue", ch);


        return "/admin/Products_View";
    }

    @GetMapping("/deleteProduct/{id}")
    public String deleteProduct(@PathVariable int id, HttpSession session){
        Boolean deleteProduct =  productService.deleteProduct(id);
        if(deleteProduct){
            session.setAttribute("Success", "Product Deleted Successfully");
        }
        else {
            session.setAttribute("Error", "Something went wrong!!!");
        }

        return "redirect:/admin/viewProducts";
    }

    @GetMapping("/editProduct/{id}")
    public String EditProduct(@PathVariable int id, Model m){

        m.addAttribute("product", productService.getProductById(id));
        m.addAttribute("categories",categoryService.getAllCategory());
        return "admin/Edit_product";
    }


    @PostMapping("/updateProduct")
    public String updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image, HttpSession session) {

        if (product.getDiscount() < 0 || product.getDiscount() > 100){

            session.setAttribute("Error", "Invalid Discount!!!");
        }else {
            Product updatedProduct = productService.updateProduct(product, image);
            if (!ObjectUtils.isEmpty(updatedProduct)) {
                session.setAttribute("Success", "Product Updated Successfully");
                return "redirect:/admin/viewProducts"; // Redirect to the product list
            } else {
                session.setAttribute("Error", "Something went wrong!!!");
            }
        }
        return "redirect:/admin/editProduct/" + product.getId(); // Stay on the edit page
    }


//--------------------------UserDetails---------------------------------
    @ModelAttribute
    public void getUserDetails(Principal p, Model m){

        if (p!=null){
            String email = p.getName();
            UserDetails userDetails = userService.getUserByEmail(email);
            m.addAttribute("user", userDetails);
        }

        List<Category> allActiveCategory = categoryService.getAllActiveCategory();
        m.addAttribute("category", allActiveCategory);
    }


    @GetMapping("/users")
    public String getAllUsers(Model m,@RequestParam Integer type) {
        List<UserDetails> users = null;
        if (type==1){
            users = userService.getUsers("USER");
        }
        else {
            users = userService.getUsers("ADMIN");

        }

        if (users == null || users.isEmpty()) {
            System.out.println("No users found!");
        } else {
            System.out.println("Users found: " + users.size());
            users.forEach(user ->
                    System.out.println("User Email: " + user.getEmail() + ", Role: " + user.getRole() + ", Enabled: " + user.getIsEnabled())
            );
        }

        m.addAttribute("userType",type);

        m.addAttribute("users", users);
        return "/admin/users";
    }


    @GetMapping("/updateStatus")
    public String updateUserAccountStatus(@RequestParam boolean status, @RequestParam Integer id,
                                          @RequestParam Integer type,HttpSession session){

        Boolean f = userService.updateAccountStatus(id,status);

        if(f){
            session.setAttribute("Success","Account Status Updated");
        }
        else {
            session.setAttribute("Error","Something went wrong");

        }
        return "redirect:/admin/users?type="+type;
    }


//-----------------------------Orders------------------------------

    @GetMapping("/orders")
    public String getAllOrders(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "3") Integer pageSize) {

        Page<ProductOrder> page = orderService.getAllOrdersSortedByDatePagination(pageNo, pageSize);
        m.addAttribute("orders", page.getContent());
        m.addAttribute("search", false);

        m.addAttribute("pageNo", page.getNumber());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        m.addAttribute("paramValue", "");  // Empty search parameter

        return "/admin/orders";
    }




    @PostMapping("/update-order-status")
    public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) throws Exception {

        OrderStatus[] values = OrderStatus.values();
        String status = null;
        for (OrderStatus orderSt : values) {
            if (orderSt.getId().equals(st)) {
                status = orderSt.getName();
                break;
            }
        }

        ProductOrder updateOrder = orderService.updateOrderStatus(id, status);
        commonUtil.sendMailForProductOrder(updateOrder, status);

        if (!ObjectUtils.isEmpty(updateOrder)) {
            session.setAttribute("Success", "Order Updated Successfully");
        } else {
            session.setAttribute("Error", "Something went wrong!");
        }

        System.out.println("Values:" + values);

        return "redirect:/admin/orders";
    }



//---------------------------------- Search ------------------------------------------


    @GetMapping("/search-order")
    public String searchOrder(@RequestParam String orderId, Model m, HttpSession session,
                              @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                              @RequestParam(name = "pageSize", defaultValue = "3") Integer pageSize) {

        if (orderId != null && !orderId.isEmpty()) {
            ProductOrder order = orderService.getOrderByOrderId(orderId.trim());

            if (order == null) {
                m.addAttribute("errorMessage", "No order found with ID: " + orderId);
                m.addAttribute("orderDetails", null);
            } else {
                m.addAttribute("orderDetails", order);
            }

            m.addAttribute("search", true);
            m.addAttribute("paramValue", orderId);
        } else {
            Page<ProductOrder> page = orderService.getAllOrdersSortedByDatePagination(pageNo, pageSize);
            m.addAttribute("orders", page.getContent());
            m.addAttribute("search", false);

            m.addAttribute("pageNo", page.getNumber());
            m.addAttribute("pageSize", pageSize);
            m.addAttribute("totalElements", page.getTotalElements());
            m.addAttribute("totalPages", page.getTotalPages());
            m.addAttribute("isFirst", page.isFirst());
            m.addAttribute("isLast", page.isLast());
            m.addAttribute("paramValue", "");
        }

        return "/admin/orders";
    }


//------------------------------Add Admin--------------------------------------------

    @GetMapping("/add-admin")
    public String loadAdminAdd(){

        return "/admin/add_admin";
    }


    @PostMapping("/save-admin")
    public String saveAdmin(UserDetails user,
                           @RequestParam("file") MultipartFile file,
                           HttpSession session) throws IOException {
        if (!file.isEmpty()) {
            // Save the uploaded file to the server
            File saveFile = new ClassPathResource("static/img").getFile();
            Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "Profile" + File.separator + file.getOriginalFilename());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            // Set the filename (not the file itself) in the database
            user.setProfileImage(file.getOriginalFilename());
        } else {
            // If no file is uploaded, use a default image
            user.setProfileImage("default.jpg");
        }

        // Save the user to the database
        UserDetails savedUser = userService.saveAdmin(user);

        if (!ObjectUtils.isEmpty(savedUser)) {
            session.setAttribute("Success", "User registered successfully!");
        } else {
            session.setAttribute("Error", "Something went wrong!");
        }

        return "redirect:/admin/add-admin";
    }

    @GetMapping("/profile")
    public String profile(){
        return "/admin/profile";
    }


    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute UserDetails user,
                                @RequestParam(value = "image", required = false) MultipartFile image,
                                HttpSession session) throws IOException {

        UserDetails updateUserProfile = userService.updateUserProfile(user, image);
        if (ObjectUtils.isEmpty(updateUserProfile)){
            session.setAttribute("Error", "Profile not updated");
        } else {
            session.setAttribute("Success", "Profile updated");
        }

        return "redirect:/admin/profile";
    }


// ---------------------Change Password-------------------------------------

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String newPassword,@RequestParam String currentPassword, Principal p, HttpSession session){

        UserDetails loggedInUserDetails = commonUtil.getLoggedInUserDetails(p);

        boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());
        if (matches){
            String encodePassword = passwordEncoder.encode(newPassword);
            loggedInUserDetails.setPassword(encodePassword);
            UserDetails updateUser = userService.updateUser(loggedInUserDetails);
            if (ObjectUtils.isEmpty(updateUser)){
                session.setAttribute("Error","Password not updated || Try Again!!!");
            }
            else {
                session.setAttribute("Success","Password updated successfully");
            }
        }else {
            session.setAttribute("Error","Current Password is Incorrect");
        }

        return "redirect:/admin/profile";
    }


}


