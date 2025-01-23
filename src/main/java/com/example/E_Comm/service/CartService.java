package com.example.E_Comm.service;

import com.example.E_Comm.model.Cart;

import java.util.List;

public interface CartService {

    public Cart saveCart(Integer productId, Integer userId);

    public List<Cart> getCartByUser(Integer userId);

}
