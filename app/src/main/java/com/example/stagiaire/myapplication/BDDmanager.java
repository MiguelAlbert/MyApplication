package com.example.stagiaire.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BDDmanager {
    public static final String TABLE_NAME = "table_localisation";
    public static final String KEY_ID_LOCALISATION="id_localisation";
    public static final String KEY_NOM_LOCALISATION="nom_localisation";
    public static final String CREATE_TABLE_LOCALISATION = "CREATE TABLE "+TABLE_NAME+
            " (" +
            " "+KEY_ID_LOCALISATION +" INTEGER primary key," +
            " "+KEY_NOM_LOCALISATION+" TEXT" +
            ");";
    private MySQLite maBaseSQLite; // notre gestionnaire du fichier SQLite
    private SQLiteDatabase db;

    // Constructeur
    public BDDmanager(Context context)
    {
        maBaseSQLite = MySQLite.getInstance(context);
    }

    public void open()
    {
        //on ouvre la table en lecture/écriture
        db = maBaseSQLite.getWritableDatabase();
    }

    public void close()
    {
        //on ferme l'accès à la BDD
        db.close();
    }

    public long addLocalisation(BDDlocalisation bddlocalisation) {
        // Ajout d'un enregistrement dans la table

        ContentValues values = new ContentValues();
        values.put(KEY_NOM_LOCALISATION, bddlocalisation.getNom_localisation());

        // insert() retourne l'id du nouvel enregistrement inséré, ou -1 en cas d'erreur
        return db.insert(TABLE_NAME,null,values);
    }

    public int modLocalisation(BDDlocalisation bddlocalisation) {
        // modification d'un enregistrement
        // valeur de retour : (int) nombre de lignes affectées par la requête

        ContentValues values = new ContentValues();
        values.put(KEY_NOM_LOCALISATION, bddlocalisation.getNom_localisation());

        String where = KEY_ID_LOCALISATION+" = ?";
        String[] whereArgs = {bddlocalisation.getId_localisation()+""};

        return db.update(TABLE_NAME, values, where, whereArgs);
    }

    public int supLocalisation(BDDlocalisation bddlocalisation) {
        // suppression d'un enregistrement
        // valeur de retour : (int) nombre de lignes affectées par la clause WHERE, 0 sinon

        String where = KEY_ID_LOCALISATION+" = ?";
        String[] whereArgs = {bddlocalisation.getId_localisation()+""};

        return db.delete(TABLE_NAME, where, whereArgs);
    }

    public BDDlocalisation getLocalisation(int id) {
        // Retourne la localistion dont l'id est passé en paramètre

        BDDlocalisation a=new BDDlocalisation(0,"");

        Cursor c = db.rawQuery("SELECT * FROM "+TABLE_NAME+" WHERE "+KEY_ID_LOCALISATION+"="+id, null);
        if (c.moveToFirst()) {
            a.setId_localisation(c.getInt(c.getColumnIndex(KEY_ID_LOCALISATION)));
            a.setNom_localisation(c.getString(c.getColumnIndex(KEY_NOM_LOCALISATION)));
            c.close();
        }

        return a;
    }

    public Cursor getLocalisation() {
        // sélection de tous les enregistrements de la table
        return db.rawQuery("SELECT * FROM "+TABLE_NAME, null);
    }

    public void deleteAll()
    {
        db = maBaseSQLite.getWritableDatabase();
        db.execSQL("delete from "+ TABLE_NAME);
        db.close();
    }
}
