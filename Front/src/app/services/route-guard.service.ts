import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { SnackbarService } from './snackbar.service';
import { GlobalConstants } from '../shared/global-constants';
import jwt_decode from 'jwt-decode';


@Injectable({
  providedIn: 'root'
})
export class RouteGuardService {

  constructor(public auth:AuthService,
    public router:Router,
    private snackbarServices:SnackbarService) { }

    canActivate(route:ActivatedRouteSnapshot):boolean{
      let expctedRoleArray = route.data;
      expctedRoleArray = expctedRoleArray.expectedRole;

      const token:any = localStorage.getItem('token');
      
      var tokenPayload:any;
      try{
        tokenPayload = jwt_decode(token);
        
      }catch(err){
        localStorage.clear();
        this.router.navigate(['/']);
      }
     
      let expectedRole = '';

      for(let i=0; i<expctedRoleArray.length; i++){
        if(expctedRoleArray[i] == tokenPayload.role){
          expectedRole = tokenPayload.role;
        }
      }
       
       if(tokenPayload.role == 'user' || tokenPayload.role == 'admin'){
        if(this.auth.isAuthenticated() && tokenPayload.role == expectedRole){
          return true;
        }
        this.snackbarServices.openSnackBar(GlobalConstants.unauthorized, GlobalConstants.error);
        this.router.navigate(['/myshop/dashboard']);
        return false;
       }else{
        this.router.navigate(['/']);
        localStorage.clear();
        return false;
       }

    }
}

