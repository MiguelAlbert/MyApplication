package com.example.stagiaire.myapplication;

public class BDDlocalisation {
    private int id_localisation;
    private String enregistrement_localisations;


    // Constructeur
    public BDDlocalisation(int id,String nom) {
        this.id_localisation=id;
        this.enregistrement_localisations = nom;
    }
    public int getId_localisation() {
        return id_localisation;
    }

    public void setId_localisation(int id) {
        this.id_localisation = id;
    }

    public String getNom_localisation() {
        return enregistrement_localisations;
    }

    public void setNom_localisation(String nom) {
        this.enregistrement_localisations = nom;
    }

}
