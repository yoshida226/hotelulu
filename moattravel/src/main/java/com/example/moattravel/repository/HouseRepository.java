package com.example.moattravel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.moattravel.entity.House;

@Repository
public interface HouseRepository extends JpaRepository<House, Integer>{

}